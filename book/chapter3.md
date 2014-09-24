#第三章 Trident拓扑与传感器数据

在这一章,我们将探索Trident拓扑。Trident提供了一个在Storm之上更高级别的抽象。Tridewnt进行了抽象事务处理和状态管理的细节。具体来说,Trident把多个元组组成为为一组离散的事务。此外,Trident提供了抽象,允许对拓扑数据执行功能,过滤和聚合操作。

我们将使用传感器数据作为例子来获得更好的理解Trident。通常,读取传感器数据流形式,从许多不同的地方。一些传统的例子包括天气或交通信息,但该模式包括到广泛的来源。例如,手机上运行的应用程序生成大量的事件信息。处理手机事件流是传感器数据处理的另一个实例。

传感器数据包含了很多设备发出的事件,往往形成一个无休止的流。这是一个完美的Storm的用例。
在这一章,我们将介绍:
· Trident拓扑
· Trident spouts
· Trident操作 – filters and functions
· Trident aggregators – Combiners and Reducers
· Trident状态

##说明我们的例子

为了更好地理解Trident拓扑,以及用Storm处理传感器数据,我们将实现一个Trident拓扑收集医学报告来识别疾病的爆发。

拓扑将处理包含以下的诊断信息事件:

<table>
    <tbody>
       <tr><th><em>Latitude</em></th><th><em>Longitude</em></th><th><em>Timestamp</em></th><th><em>Diagnosis Code (ICD9-CM)</em></th></tr>
       <tr><td>39.9522</td><td>-75.1642</td><td>03/13/2013 at 3:30 PM</td><td>320.0 (Hemophilus meningitis)</td></tr>
       <tr><td>40.3588</td><td>-75.6269</td><td>03/13/2013 at 3:50 PM</td><td>324.0 (Intracranial abscess)</td></tr>
    </tbody>
</table>

每个事件将包括发生的全球定位系统(GPS)的坐标。指定的纬度和经度是十进制格式。事件还包含ICD9-CM代码,代表事件的诊断和一个时间戳。ICD-9-CM代码的完整列表可以在查看:http://www.icd9data.com/ .

为了检测爆发,系统将计算特定疾病编码在一个地理位置在指定的一段时间的出现频次。为了简化这个示例,我们将每个诊断事件映射到最近的城市。在实际的系统中,您很可能会执行更复杂的事件地理空间聚类。

本例中,我们将以来出现的小时分组。在一个真实世界的系统,你最有可能使用滑动窗口来计算移动平均线
趋势。

最后,我们将使用一个简单的阈值,以确定是否有爆发。如果一个小时内的出现数的大于阈值,系统将会发送一个报警并发给国家警卫队。

为了维护历史记录,我们也将持久化对于每一个城市,每小时,每种疾病出现的数量。

##Trident拓扑

为了满足这些需求,我们将需要一个统计出现次数拓扑。这可能是一个挑战,而使用标准Storm拓扑因为元组可以重播,导致重复计算。正如我们在接下来的几节中看到的,Trident提供原语来解决这个问题。

我们将使用拓扑如下:

![Trident Topology](./pic/3/trident_topology.jpg)

前面拓扑的代码如下:

    public class OutbreakDetectionTopology {
        public static StormTopology buildTopology() {
            TridentTopology topology = new TridentTopology();
            DiagnosisEventSpout spout = new DiagnosisEventSpout();
            Stream inputStream = topology.newStream("event",spout);

            // Filter for critical events.
            inputStream.each(new Fields("event"), new DiseaseFilter()))
            // Locate the closest city
            .each(new Fields("event"), new CityAssignment(), new Fields("city"))
            // Derive the hour segment
            .each(new Fields("event", "city"), new HourAssignment(), new Fields("hour","cityDiseaseHour"))
            // Group occurrences in same city and hour
            .groupBy(new Fields("cityDiseaseHour"))
            // Count occurrences and persist the results.
            .persistentAggregate(new OutbreakTrendFactory(), new Count(), new Fields("count"))
            .newValuesStream()
            // Detect an outbreak
            .each(new Fields("cityDiseaseHour","count"), new OutbreakDetector(), new Fields("alert"))
            // Dispatch the alert
            .each(new Fields("alert"), new DispatchAlert(), new Fields());
        }
    }

前面的代码显示了不同的Trident函数之间的连接。首先,DiagnosisEventSpout函数发出事件。事件然后由DiseaseFilter函数过滤,它过滤掉出现的我们不关心的疾病,。在那之后,CityAssignmentfunction使事件与城市相关联。然后,HourAssignment函数分配一小时事件添加一个元组的可以,它包括城市,小时,和疾病的代码。然后我们按这个关键分组,使计数的并持久化这些计数在拓扑persistAggregatefunction一步。然后把计数传递到OutbreakDetectorfunction进行阈值计算,超过阈值时发出警报。最后,DispatchAlert函数接收警报，产生日志消息,并终止程序。在下一节中,我们将深入学习每一个步骤。

##Trident spouts

让我们先看看拓扑的spout。与Storm相比,Trident引入了批次的概念。不像Storm spout,Trident Spout必须批量发出元组。

每批数据都有自己的唯一事务标识符。一个Spout决定一批数据组成通过基于合同的约束。有三种约定类型的Spout:非事务性、事务型和不透明。

非事务性Spout无法保证批次的构成，消息可能重叠。两个不同批次可能包含相同的元组。事务型Spout不会有重叠,保证同一批次总是包含相同的元组。不透明Spout保证批次非重叠,但同一批可能会改变内容。

如下表中所示:

<table>
    <tbody>
       <tr><th><em>Spout type</em></th><th><em>Batches may overlap</em></th><th><em>Batch contents may change</em></th></tr>
       <tr><td>Non-transactional</td><td>X</td><td>X</td</tr>
       <tr><td>Opaque</td><td>X</td><td></td></tr>
       <tr><td>Transactional</td><td></td><td></td></tr>
    </tbody>
</table>

Spout的接口如下面的代码片段:

    public interface ITridentSpout<T> extends Serializable {
        BatchCoordinator<T> getCoordinator(String txStateId, Map conf,    TopologyContext context);
        Emitter<T> getEmitter(String txStateId, Map conf, TopologyContext context);
        Map getComponentConfiguration();
       Fields getOutputFields();
    }

在Trident中,Spout并不实际发出元组。相反,工作由BatchCoordinator和Emiter来完成。Emitter的功能是负责发射元组,而BatchCoordinator函数负责管理批处理和元数据,这样发射器就能够正确地重放批数据。

TridentSpout函数简单地提供了BatchCoordinator和发射器的访问器方法功能和声明了Spout将发出字段。下面用DiagnosisEventSpout来说明功能:

    public class DiagnosisEventSpout implements ITridentSpout<Long> {
        private static final long serialVersionUID = 1L;
        SpoutOutputCollector collector;
        BatchCoordinator<Long> coordinator = new DefaultCoordinator();
        Emitter<Long> emitter = new DiagnosisEventEmitter();
    
        @Override
        public BatchCoordinator<Long> getCoordinator(
                String txStateId, Map conf, TopologyContext
                context) {
            return coordinator;
        }
    
        @Override
        public Emitter<Long> getEmitter(String txStateId, Map conf, TopologyContext context) {
            return emitter;
        }
    
        @Override
        public Map getComponentConfiguration() {
            return null;
        }
    
        @Override
        public Fields getOutputFields() {
            return new Fields("event");
        }
    }


在我们的示例拓扑中，getOutputFields()方法在前面的代码中,,Spout发出一个称为事件的字段,其中包含DiagnosisEvent类中。

BatchCoordinator类实现以下接口:

    public interface BatchCoordinator<X> {
        X initializeTransaction(long txid, X prevMetadata);
        void success(long txid);
        boolean isReady(long txid);
        void close();
    }

BatchCoordinator类是一个泛型类。泛型类是重播一批所需的元数据。在我们的示例中,Spout发出随机事件,因此元数据将被忽略。然而,在实际的系统中,元数据可能包含组成一批消息的标识符或对象。有了这些信息,不透明和事务性Spout可以遵守合同的内容,并确保批次不重叠,在事务Spout情况下,批处理不改变内容。

BatchCoordinator类在一个Storm bolt单线程中实现了一个操作。Storm持久化元数据在Zookeeper中。当每个事务完成时它通知协调员。

在我们的例子中,如果我们不协调,下面是协调用于DiagnosisEventSpout类:

    public class DefaultCoordinator implements
            ITridentSpout.BatchCoordinator<Long>, Serializable {
        private static final long serialVersionUID = 1L;
        private static final Logger LOG =
                LoggerFactory.getLogger(DefaultCoordinator.class);
    
        @Override
        public boolean isReady(long txid) {
            return true;
        }
    
        @Override
        public void close() {
        }
    
    
        @Override
        public Long initializeTransaction(long txid, Long prevMetadata, Long currMetadata) {
            return null;
        }
    
        @Override
        public void success(long txid) {
            LOG.info("Successful Transaction [" + txid + "]");
        }
    }

在Trident Spout中，第二部分是发射器的功能。发射器功能完成Storm Spout的功能使用收集器发出元组。唯一的区别是,它使用一个TridentCollector类,元组必须包含在一个批处理被BatchCoordinator类初始化。

发射器的接口函数看起来像下面的代码片段:

    public interface Emitter<X> {
        void emitBatch(TransactionAttempt tx, X coordinatorMeta, TridentCollector collector);
        void close();
    }


前面的代码所示,发射器函数只有一个职责——给定批发出的元组。要做到这一点,批处理的功能是传递元数据(这是由协调器),关于事务的信息给收集器,发射器函数使用发出的元组。DiagnosisEventEmitter类的实现如下:

    public class DiagnosisEventEmitter implements
            ITridentSpout.Emitter<Long>, Serializable {
        private static final long serialVersionUID = 1L;
        AtomicInteger successfulTransactions = new AtomicInteger(0);
    
        @Override
        public void emitBatch(TransactionAttempt tx, Long
                coordinatorMeta, TridentCollector collector) {
            for (int i = 0; i < 10000; i++) {
                List<Object> events = new ArrayList<Object>();
                double lat =  new Double(-30 + (int) (Math.random() * 75));
                double lng =  new Double(-120 + (int) (Math.random() * 70));
                long time = System.currentTimeMillis();
                String diag = new Integer(320 + (int) (Math.random() * 7)).toString();
                DiagnosisEvent event = new DiagnosisEvent(lat, lng, time, diag);
                events.add(event);
                collector.emit(events);
            }
        }
    
        @Override
        public void success(TransactionAttempt tx) {
            successfulTransactions.incrementAndGet();
        }
    
        @Override
        public void close() {
        }
    }

在emitBatch()方法中完成我们的工作。对于这个示例,我们将随机分配一个经度和纬度,保持在美国左右
,我们将使用System.currentTimeMillis()方法产生诊断的时间戳。


在现实生活中,ICD-9-CM码分布在范围在000年和999年之间。对于本例,我们将只使用诊断代码在320和327之间。这些代码列出如下:

<table>
    <tbody>
       <tr><th><em>Code</em></th><th><em>Description</em></th></tr>
       <tr><td>320</td><td>Bacterial meningitis</td></tr>
       <tr><td>321</td><td>Meningitis due to other organisms</td></tr>
       <tr><td>322</td><td>Meningitis of unspecified cause</td></tr>
       <tr><td>323</td><td>Encephalitis myelitis and encephalomyelitis</td></tr>
       <tr><td>324</td><td>Intracranial and intraspinal abscess</td></tr>
       <tr><td>325</td><td>Phlebitis and thrombophlebitis of intracranial venous sinuses</td></tr>
       <tr><td>326</td><td>Late effects of intracranial abscess or pyogenic infection</td></tr>
       <tr><td>327</td><td>Organic sleep disorders</td></tr>
    </tbody>
</table>

这些诊断代码之随机分配给一个事件。

在这个例子中,我们将使用一个对象来封装诊断事件。只是为了容易,我们可以发出的每个组件作为一个单独的字段元组。对象封装和使用tuple中的字段之间有一个平衡。通常,这是一个好主意保持字段的数量降低到一个可控制的范围后,但它也包括数据使用的是有意义的控制流和/或分组的字段元组。

在我们的示例中,DiagnosisEvent类是拓扑的关键操作数据。该对象看起来像下面的代码片段:

    public class DiagnosisEvent implements Serializable {
        private static final long serialVersionUID = 1L;
        public double lat;
        public double lng;
        public long time;
        public String diagnosisCode;
    
        public DiagnosisEvent(double lat, double lng,
                              long time, String diagnosisCode) {
            super();
            this.time = time;
            this.lat = lat;
            this.lng = lng;
            this.diagnosisCode = diagnosisCode;
        }
    }

对象是一个简单的JavaBean。时间存储成为一个Long型变量自纪元依赖。纬度和经度都存储为double型。
diagnosisCode存储为一个字符串,以防系统需求能够处理其他类型的代码不基于ICD-9等字母数字代码。

在这一点上,拓扑能够发出事件。在实际实现中,我们可能把拓扑集成到一个医疗索赔处理引擎或电子健康记录系统中。

##Trident操作--filters and functions