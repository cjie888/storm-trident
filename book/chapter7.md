#第七章 集成Druid进行金融分析 

在这一章,我们将扩展使用Trident来创建一个实时金融分析仪表盘。该系统将处理金融消息提供各级粒度股票价格信息随着时间的推移。系统将展示与事务性系统集成，使用自定义状态实现。

在前面的示例中,我们使用Trident统计过去一段时间运行事件的总数。这个简单的例子充分说明了分析一位数据,但是架构设计不灵活。引入一个新的维度需要重新Java开发和部署新的代码。

传统上,数据仓库技术和商业智能平台是用于计算和存储空间分析。仓库部署的联机分析处理(OLAP)系统,与在线事务处理(OLTP)分离。数据传播到OLAP系统,但通常有一些滞后。这是一个足够的回顾性分析模型,但不满足的情况下,需要实时分析。

同样,其他方法使用批处理技术产生了数据科学家。数据科学家使用Pig语言来表达他们的查询。然后,这些查询编译成工作,运行在大型的数据集上。幸运的是,他们运行在Hadoop等分布式平台上跨多台机器处理,但这仍然引入了大量的延迟。

这两种方法对金融系统都不适应,这种滞后的可用性分析系统不能热搜。仅旋转一个批处理工作开销可能太多的延迟的实时金融体系的要求。

在这一章,我们将扩展我们的Storm的使用来提供一个灵活的系统,引入新的维度,仅需要很少的努力
同时提供实时分析。我们的意思是说,只有一个短延迟数据摄入和可用性之间的维度分析。

在这一章,我们将讨论下列主题:

1. 定制状态实现
1. 与非事务性存储的集成
1. 使用Zookeeper保存分布式状态
1. Druid和实时汇总分析

##用例

在我们的用例中,我们将利用一个金融体系中股票的订单。使用这些信息,我们将提供随着时间的推移的价格信息,这是可以通过表示状态传输(REST)接口来完成。

在金融行业规范化消息格式是金融信息交换(FIX)格式。这种格式的规范可以参考: http://www.fixprotocol.org/。

An example FIX message is shown as follows:
一个FIX信息的例子如下:

	23:25:1256=BANZAI6=011=135215791235714=017=520=031=032=037=538=1000039=054=155=SPY150=2151=010=2528=FIX.4.19=10435=F34=649=BANZAI52=20121105-

FIX信息本质上是键-值对流。ASCII字符01,表示开始信息头开始(SOH)。FIX是指键作为标记。如前面所示信息,标签被识别为整数。每个标签都有一个关联的字段名称和数据类型。一个完整的
参考标记类型可访问 http://www.fixprotocol.org/FIXimate3.0/en/FIX.4.2/fields_sorted_by_tagnum.html。

用例的重要字段如下表所示:

<table>
    <tbody>
       <tr><th><em>Tag ID</em></th><th><em>Field name</em></th><th><em>Description</em></th><th><em>Data type</em></th></tr>
       <tr><td>11</td><td>CIOrdID</td><td>This is the unique identifier for message.</td><td>String</td></tr>
       <tr><td>35</td><td>MsgType</td><td>This is is the type of the FIX message.</td><td>String</td></tr>
       <tr><td>44</td><td>Price</td><td>This is the stock price per share.</td><td>Price</td></tr>
       <tr><td>55</td><td>Symbol</td><td>This is the stock symbol. </td><td>String</td></tr>
    </tbody>
</table>

FIX是一个TCP/IP协议上的层。因此,在实际系统中,这些消息通过TCP/IP接收。与Storm易于集成，
系统可以是Kafka队列的消息。然而,对于我们的示例,我们只会摄取FIX消息文件。FIX支持多种消息类型。一些是控制消息(例如登录,心跳,等等)。我们将过滤掉这些消息,只传递类型（包括价格信息）到分析引擎。

##实现一个非事务系统

基于扩展前面的例子,我们可以开发一个框架配置允许用户以指定的维度来聚集他们需要的事件。然后,我们可以在我们的拓扑中使用该配置来维护一组内存中的数据集进行累积聚合,但任何内存存储容易故障。为了解决容错系统,我们可以使这些聚合保存在数据库中。

我们需要预测和支持所有不同用户想要完成的类型的聚合(例如,金额、平均、地理空间等)。这似乎是一个很困难的工作。

幸运的是,我们可以选择实时分析引擎。一个流行的开源的选择是Druid。下面的文章是在他们的白皮书（ http://static.druid.io/docs/druid.pdf ）找到的:

Druid是一个开源的、实时分析数据存储,支持快速特别对大规模数据集的查询。系统使用面向列数据布局,无共享架构,高级索引结构,允许任意检索百亿条记录的表达到秒级延迟。Druid支持水平扩展, 使用Metamarkets数据分析平台核心引擎。

从这个摘录看,Druid完全符合我们的要求。现在,面临的挑战是怎么与Storm结合。

Druid的技术堆栈自然符合基于Storm的生态系统。像Storm一样,它使用ZooKeeper协调之间的节点。Druid也支持直接与Kafka的集成。在某些情况下,这可能是合适的。在我们的示例中,为了演示非事务性系统的集成,我们将直接将Druid与Storm集成。

我们这里将包括Druid的简要描述。然而,对于更详细Druid的信息,请参考下面的网站: https://github.com/metamx/druid/wiki

Druid通过实时节点收集信息。基于一个可配置的粒度,Real-time节点收集事件信息,然后永久保存在一个很深的叫做段的存储机制。Druid持续将这些元数据的元数据存储在MySQL。

主节点识别新的段，基于规则确定计算节点段,并通知计算节点的推送到新的段。Broker结点在计算节点的前面,接收其他消费者的REST查询并分发这些查询到适当的计算节点。

因此,,集成了Storm与Druid的一个架构看起来类似于什么下图所示:

![architecture](./pic/7/financial_architecture.png)

如在前面的图中,有三种数据存储机制。MySQL数据库是一个简单的元数据存储库。它包含所有的元数据信息的所有部分。深度存储机制包含实际的段信息。每个段包含一个混合索引为一个特定的事件时间基于维度和聚合在一个配置文件中定义。因此,一个段可以很大大(例如,2 GB blob)。在我们的示例中,我们将使用Cassandra作为深度存储机制。

最后,第三个数据存储机制是ZooKeeper。存储在ZooKeeper是临时的,仅用于控制信息。当一个新的段是可用的,Master节点写一个短暂的节点在ZooKeeper。计算节点订阅了相同的路径,短暂的节点触发计算节点当有新的段时。当段被成功地检索后,计算节点从ZooKeeper删除该节点。

在我们的例子中,整个的事件序列如下:

![Event Sequence](./pic/7/event_sequence.jpg)

前面的图展示了Storm事件处理下游。什么是重要的认识在许多实时分析引擎是无法恢复一个事务的。分析系统是高度优化处理速度和聚合过程。牺牲了事务完整性。

如果我们重新审视Trident的状态分类,有三种不同种类的状态:事务、不透明和非事物性。事务状态需要不断持久化每一批的内容。一个不透明的事务状态可以容忍批改写改变随着时间的推移。最后,一个非事务性状态不能保证完全的只处理一次语义。

总结storm.trident.state状态对象的Javadoc,有三种不同的状态:

<table>
    <tbody>
       <tr><td>Non-Transactional state</td><td>In this state, commits are ignored.No rollback can be done.Updates are permanent.</td></tr>
       <tr><td>Repeat Transactional state</td><td>The system is idempotent as long as all batches are identical.</td></tr>
       <tr><td>Opaque Transactional state</td><td>State transitions are incremental. The previous state is stored along with the batch identifier to tolerate changing batch composition in the event of replay.</td></tr>
    </tbody>
</table>

重要的是要意识到引入拓扑的状态要有效的保证任何写入存储的序列。这可能大大影响性能。如果可能的话,最好的方法是确保整个系统是幂等的。如果写都是幂等的,那么你不需要引入事务性存储(或状态),因为架构自然地容忍元组重发。

通常,如果状态的持久性是由你控制的数据库模式,你可以调整模式添加额外的信息参与事务:上次提交的批处理标识符为重复事务和以前的状态不透明的事务。然后,在状态实现时,您可以利用这些信息来确保你的状态对象符合您正在使用的类型Spout。

然而,这并非总是如此,特别是在系统执行聚合,如计数、求和、求平均值等等时。计数器在Cassandra中有这个约束机制。不可能取消一个计数器,并使加法幂等是不可能的。如果一个元组进行重播,计数器再次增加,你最有可能在数量上超过系统中的元素。出于这个原因,任何状态实现了Cassandra计数器是非事务性的支持。

同样,Druid是非事务性的。一旦Druid消费一个事件,事件将无法撤销。因此,如果一批在Storm部分被Druid消费,然后批重播,或成分变化,根本没有维度分析恢复方式。出于这个原因,有趣的是考虑Druid和Storm之间的集成，为了解决回放步骤,这种耦合的力量。

简而言之,Storm连接到Druid,我们将利用的特征事务Spout连接时所说的风险降到最低像Drudi非事务性状态机制。

##拓扑

了解了架构概念后,让我们回到这个用例。为了保持专注于集成,我们将保持拓扑结构简单。下图描述了拓扑:

![Druid Topology](./pic/7/druid_topology.png)

包含简单FIX消息的FIX Spout发出元组信息。然后过滤给定类型信息,过滤包含价格信息的库存订单。然后,这些过滤的元组流入DruidState对象,与Druid的桥。

这个简单的拓扑结构的代码如下:
	
	public class FinancialAnalyticsTopology {

		public static StormTopology buildTopology() {
	
			TridentTopology topology = new TridentTopology();
			FixEventSpout spout = new FixEventSpout();
			Stream inputStream = topology.newStream("message", spout);
			inputStream.each(new Fields("message"),	new MessageTypeFilter()).partitionPersist(new DruidStateFactory(),new Fields("message"), new DruidStateUpdater());
			return topology.build();
		}
	}

###Spout

有许多FIX消息格式的解析器。在Spout中,我们将使用FIX Parser,这是一个Google项目。更多项目信息,你可以参考 https://code.google.com/p/fixparser/。

就像前一章,Spout本身很简单。它只是返回一个协调器和一个发射器的引用,如下面所示代码:

	public class FixEventSpout implements ITridentSpout<Long> {
		private static final long serialVersionUID = 1L;
		SpoutOutputCollector collector;
		BatchCoordinator<Long> coordinator = new DefaultCoordinator();
		Emitter<Long> emitter = new FixEventEmitter();
		...
		@Override
		public Fields getOutputFields() {
		    return new Fields("message");
		}
	}

正如前面的代码所示,Spout声明一个输出字段:message。这将包含发射器产生的FixMessageDto对象,如以下代码所示:

	public class FixEventEmitter implements ITridentSpout.Emitter<Long>,
	        Serializable {
	    private static final long serialVersionUID = 1L;
	    public static AtomicInteger successfulTransactions = new AtomicInteger(0);
	    public static AtomicInteger uids = new AtomicInteger(0);
	
	    @SuppressWarnings("rawtypes")
	    @Override
	    public void emitBatch(TransactionAttempt tx, Long coordinatorMeta, TridentCollector collector) {
	        InputStream inputStream = null;
	        File file = new File("fix_data.txt");
	        try {
	            inputStream = new BufferedInputStream(new FileInputStream(file));
	            SimpleFixParser parser = new SimpleFixParser(inputStream);
	            SimpleFixMessage msg = null;
	            do {
	                msg = parser.readFixMessage();
	                if (null != msg) {
	                    FixMessageDto dto = new FixMessageDto();
	                    for (TagValue tagValue : msg.fields()) {
	                        if (tagValue.tag().equals("6")) { //AvgPx
	                            // dto.price = Double.valueOf((String) tagValue.value());
	                            dto.price = new Double((int)(Math.random() * 100));
	                        } else if (tagValue.tag().equals("35")) {
	                            dto.msgType = (String) tagValue.value();
	                        } else if (tagValue.tag().equals("55")) {
	                            dto.symbol = (String) tagValue.value();
	                        } else if (tagValue.tag().equals("11")) {
	                            // dto.uid = (String) tagValue.value();
	                            dto.uid = Integer.toString(uids.incrementAndGet());
	                        }
	                    }
	                    new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(dto);
	                    List<Object> message = new ArrayList<Object>();
	                    message.add(dto);
	                    collector.emit(message);
	                }
	            } while (msg != null);
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        } finally {
	            IoUtils.closeSilently(inputStream);
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

从前面的代码中,您可以看到,我们把重新解析为每个批次。正如我们前面所述,在实时系统中我们可能会通过TCP / IP接收消息并放入Kafka队列。然后,我们将使用Kafka Spout发出消息。它是一个个人喜好问题;但是,在Storm完全封装数据处理,系统将最有可能从队列获取原始消息文本。在设计中,我们将在一个函数中解析文本而不是Spout中。

虽然这Spout对于这个示例是足够了,注意,每一批的组成是相同的。具体地说,每一批包含所有从文件的消息。因为我们状态的设计依赖于这一特点,在实际系统中,我们需要使用TransactionalKafkaSpout。

###Filter

像Spout,过滤器是非常简单的。它检查msgType对象和过滤不完整订单的消息。完整订单实际上是股票
购买收据。它们包含的均价格,贸易和执行购买股票的符号。下面的代码是过滤消息类型:

	public class MessageTypeFilter extends BaseFilter {
	    private static final long serialVersionUID = 1L;
	
	    @Override
	    public boolean isKeep(TridentTuple tuple) {
	        FixMessageDto message = (FixMessageDto) tuple.getValue(0);
	        if (message.msgType.equals("8")) {
	            return true;
	        }
	        return false;
	    }
	}

这提供了一个很好的机会,指出可串行性的重要性在Storm中。请注意,在前面的代码中过滤操作在一个FixMessageDto对象。这将是更容易简单地使用SimpleFixMessage对象,但SimpleFixMessage对象不是可序列化的。

这不会产生任何问题当运行在本地集群。然而,由于storm在数据处理中元组在主机之间交换,所有的元素在一个元组必须是可序列化的。

###Tip

开发人员经常提交修改元组的数据对象是没有序列化的。这将导致下游部署问题。确保所有元组中的对象是可序列化的,添加单元测试,验证对象是可序列化的。测试是很简单的,使用以下代码:

	new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(YOUR_OBJECT);

###状态设计

现在,让我们继续这个例子的最有趣的方面。为了集成Storm与Druid,我们将嵌入实时服务器在我们的Druid拓扑结构并实现必要的接口连接元组流。为了减少连接到非事务性系统固有的风险,我们利用Zookeepr保存状态信息。持久化不会阻止异常由于失败,但它将有助于确定哪些数据存在风险当发生故障时。

高层设计如下所示:

![High Level Design](./pic/7/high_level_design.png)

在高级别上,Storm用一个工厂创建状态对象在workerJVM进程中。每一批次为每个分区创建一个状态对象。状态工厂对象确保实时服务器运行在它返回任何状态对象并启动服务，如果没有运行。状态对象然后将这些消息放入缓冲区,直到Storm要求提交。当Storm调用提交方法师,状态对象解锁Druid Firehose。这将信号发送给Druid,准备数据聚合。然后,我们在storm commit方法中阻塞,而实时服务器开始拉数据通过Firehose。

为了确保每个分区最多被一次处理,我们将为每个分区设置一个分区标识符。分区标识符包括批处理标识符和分区索引,并唯一地标识一组数据,因为我们使用的是事务spout。

在Zookeeper中有三种状态：


<table>
    <tbody>
       <tr><th><em>State</em></th><th><em>Description</em></th></tr>
       <tr><td>inProgress</td><td>This Zookeeper path contains the partition identifiers that Druid is processing.</td></tr>
       <tr><td>Limbo</td><td>This Zookeeper path contains the partition identifiers that Druid consumed in their entirety, but which may not be committed.</td></tr>
       <tr><td>Completed</td><td>This Zookeeper path contains the partition identifiers that Druid successfully committed.</td>
    </tbody>
</table>

当一批是在处理过程中,Firehose写分区标识符到inProgress路径。当Druid把整个Storm分区完全拉取时,分区标识符被移到Limbo,我们释放Storm继续处理当我们等待提交消息从Druid。

当从Druid接收到提交消息后,Firehose移动分区标识符到Completed路径。在这一点上,我们假设数据被写入磁盘。我们仍然容易丢失数据由于磁盘故障。然而,如果我们假设我们可以使用批处理重建的聚合,然后这是最有可能的一个可接受的风险。

以下状态机说明了处理的不同阶段:

![State Machine](./pic/7/state_machine.png)

如图所示,在缓冲消息和聚合信息之间有一个循环。主要控制回路开关迅速在这两个状态之间转换,分裂storm处理循环和druid聚合循环之间的时间。状态是互斥的:系统聚集一批,或者缓冲下一批。

当Druid的信息写入到磁盘时第三个状态触发。当这种情况发生时(稍后我们将看到),通知Firehose,我们可以更新持久性机制表明该操作被安全地处理。直到在调用之前被Druid提交的批次必须保留在Limbo状态。

而在Limbo状态,没有假设可以对数据改变。Druid可能有也可能没有聚合的记录。

在发生故障时,Storm可能利用其他TridentState实例来完成处理。因此,对于每个分区,Firehose必须执行以下步骤:

1. Firehose必须检查分区是否已经完成。如果是这样,分区是一个重播,很可能由于下游失败。因为批处理是保证有相同的内容,它可以安全地忽略Druid已经聚合的内容。系统可以生成一个警告消息日志。

2. Firehose必须检查分区是否在limbo状态。如果是这样的话,那么Druid完全消费分区,但从不调用提交或提交后系统失效，在Druid更新Zookeepr前。系统应该发出警报。它不应该试图完成一批,因为它完全被Druid消费,我们不知道聚集的状态。它只是返回,使Storm继续处理下一批。

3. Firehose必须检查正在处理的分区。如果是这样的话,那么出于某种原因,在网络上的某个地方,由另一个实例处理分区。这不应该发生在普通处理时。在这种情况下,系统应该为这个分区发出一个警告。在我们的简单系统,我们将简单地处理,忽略它给我们的离线批处理使之能正确的聚合。

在许多大型实时系统,用户愿意容忍轻微差异在实时分析中，只要倾斜是罕见的并且可以很快弥补。

重要的是要注意,这种方法成功是因为我们使用的是事务spout。事务spout保证每一批都有相同的成分。此外,这个方法起作用,每个分区在批处理必须有相同的成分。这是真的当且仅当拓扑分区是确定的。使用确定性分区和事务spout,每个分区将包含相同的数据,即使在事件的重演。我们使用随机分组,这种方法行不通。我们的示例拓扑是确定的。这可以保证一批标识符,加上一个分区索引时,表示一组一致的数据。

##架构实现

有了设计,我们可以将注意力转向实现。序列图如下所示:

![Sequence Diagram](./pic/7/sequence_diagram.jpg)

如前图所示的状态机实现设计。实时服务器启动后,Druid使用hasMore()方法调用StormFirehose对象。Druid的合同规定,Firehose对象实现将阻塞,直到数据是可用的。Druid是轮询，Firehose对象阻塞,Storm将元组发送到DruidState对象的消息缓冲区。批处理完成后,Storm调用DruidState对象commit()方法。这时,分区状态将被更新。分区进入progress状态，实现解锁StormFirehose对象。

Druid开始通过nextRow()方法从StormFirehose对象获取数据。当StormFirehose对象消费完分区的内容时,它将分区转到limbo装填,然后把控制全交给Storm。

最后,当StormFirehose调用commit()方法时,实现返回一个Runnable,这是Drudi使用通知Firehose持久化分区。Druid调用run()方法实现分区完成。

###DruidState

首先,我们将看看Storm的方程。在前面的章节中,我们扩展了NonTransactionalMap类来保存状态。抽象屏蔽我们的顺序批处理的细节。我们只是实现了IBackingMap接口支持multiGet和multiPut调用,和其余的超类。

在这个场景中,我们需要更多的控制持久性过程而不是提供的默认实现。相反,我们需要实现自己的基本状态接口。下面的类图描述了类层次结构:

![State Class](./pic/7/state_class.png)

图中所示,DruidStateFactory类管理嵌入式实时节点。由此可得出一个论点管理嵌入式服务器的更新。然而,由于只应该有每个JVM一个实时服务器实例,实例需要存在任何状态对象之前,嵌入式服务器的生命周期管理似乎符合工厂更自然。

下面的代码片段包含DruidStateFactory类的相关部分:

	public class DruidStateFactory implements
	        StateFactory {
	    private static final long serialVersionUID = 1L;
	    private static final Logger LOG = LoggerFactory.getLogger(DruidStateFactory.class);
	    private static RealtimeNode rn = null;
	
	    private static synchronized void startRealtime() {
	        if (rn == null) {
	            final Lifecycle lifecycle = new Lifecycle();
	            rn = RealtimeNode.builder().build();
	            lifecycle.addManagedInstance(rn);
	            rn.registerJacksonSubtype(new NamedType(StormFirehoseFactory.class, "storm"));
	            try {
	                lifecycle.start();
	            } catch (Throwable t) {
	            }
	        }
	    }
	
	    @Override
	    public State makeState(Map conf, IMetricsContext
	            metrics, int partitionIndex, int numPartitions) {
	        DruidStateFactory.startRealtime();
	        return new DruidState(partitionIndex);
	    }
	}

没有太多的细节,前面的代码以实时节点开始如果没有开始。此外,它注册StormFirehoseFactory类与实时节点。

工厂也实现了StateFactory接口从Storm,Storm可以使用这个工厂来创建新的状态对象。状态对象本身相当简单:

	public class DruidState implements State {
	    private static final Logger LOG = LoggerFactory.getLogger(DruidState.class);
	    private List<FixMessageDto> messages = new ArrayList<FixMessageDto>();
	    private int partitionIndex;
	
	    public DruidState(int partitionIndex) {
	        this.partitionIndex = partitionIndex;
	    }
	
	    @Override
	    public void beginCommit(Long batchId) {
	    }
	
	    @Override
	    public void commit(Long batchId) {
	        String partitionId = batchId.toString() + "-" + partitionIndex;
	        LOG.info("Committing partition [" +
	                partitionIndex + "] of batch [" + batchId + "]");
	        try {
	            if (StormFirehose.STATUS.isCompleted(partitionId)) {
	                LOG.warn("Encountered completed partition ["
	                        + partitionIndex + "] of batch [" + batchId + "]");
	                return;
	            } else if (StormFirehose.STATUS.isInLimbo(partitionId)) {
	                LOG.warn("Encountered limbo partition [" +
	                        partitionIndex + "] of batch [" + batchId +
	                        "] : NOTIFY THE AUTHORITIES!");
	                return;
	            } else if (StormFirehose.STATUS.isInProgress(partitionId)) {
	                LOG.warn("Encountered in-progress partition [" +
	                        partitionIndex + "] of batch [" + batchId
	                        + "] : NOTIFY THE AUTHORITIES!");
	                return;
	            }
	            StormFirehose.STATUS.putInProgress(partitionId);
	            StormFirehoseFactory.getFirehose()
	                    .sendMessages(batchId, messages);
	        } catch (Exception e) {
	            LOG.error("Could not start firehose for ["
	                    + partitionIndex + "] of batch [" + batchId + "]", e);
	        }
	    }
	
	    public void aggregateMessage(FixMessageDto message) {
	        messages.add(message);
	    }
	}


正如你所看到的在前面的代码中,状态对象是一个消息缓冲区。它转发实际提交逻辑给Firehose对象,不久我们将检查。然而,有几个关键的行在这个类中实现我们前面描述的故障检测。

状态对象commit()方法中的条件逻辑检查Zookeeper状态来确定这个分区已经成功处理(在完成状态),未能提交(在Limbo状态),或处理失败(在处理中装填)。我们将会深入研究状态存储当我们检查DruidPartitionStatus对象。

同样重要的是要注意,commit()方法由Storm直接调用,但aggregateMessage()方法被Updater调用。尽管Storm不应该同时调用这些方法,我们选择使用一个线程安全的向量。

DruidStateUpdater代码如下:

	public class DruidStateUpdater implements
	        StateUpdater<DruidState> {
	    @Override
	    public void updateState(DruidState state,
	                            List<TridentTuple> tuples, TridentCollector collector) {
	        for (TridentTuple tuple : tuples) {
	            FixMessageDto message = (FixMessageDto) tuple.getValue(0);
	            state.aggregateMessage(message);
	        }
	    }
	
	    @Override
	    public void prepare(Map conf, TridentOperationContext context) {
	    }
	
	    @Override
	    public void cleanup() {
	    }
	}

如前面的代码所示,updater只需循环遍历元组并将它们传递到缓冲区的状态对象。

###实现StormFirehose对象

之前我们将注意力转向了Druid的实现,我们应该退一步,更详细地讨论Druid。Druid feed通过规范文件配置。在我们的例子中,是 realtime.spec,如以下代码所示:

	[{
	"schema": {
		"dataSource": "stockinfo",
		"aggregators": [
			{ "type": "count", "name": "orders"},
			{ "type": "doubleSum", "fieldName": "price", "name":"totalPrice" }
		],
		"indexGranularity": "minute",
		"shardSpec": {"type": "none"}
	},
	"config": {
		"maxRowsInMemory": 50000,
		"intermediatePersistPeriod": "PT30s"
	},
	"firehose": {
		"type": "storm",
		"sleepUsec": 100000,
		"maxGeneratedRows": 5000000,
		"seed": 0,
		"nTokens": 255,
		"nPerSleep": 3
	},
	"plumber": {
		"type": "realtime",
		"windowPeriod": "PT30s",
		"segmentGranularity": "minute",
		"basePersistDirectory":
		"/tmp/example/rand_realtime/basePersist"
	}
	}]

在我们的例子中,最重要的元素在前面firehose规范文件是有模式的。模式元素定义了数据和Druid应该执行的聚合数据。在我们的示例中,Druid将计算的次数,我们看到股票代码字段和跟踪订单总价格支付的totalPrice字段。totalPrice字段将被用来计算股票平均超过时间的价格。此外,您需要指定一个indexGranularity对象,指定索引的时间粒度。

firehose元素包含Firehose的配置对象。作为StateFactory接口,我们看到一个使用Druid的FirehoseFactory实现将被注册，当实时服务器启动时。工厂注册为Jackson类型。当实时规范文件进行解析时,firehose元素的JSON类型是用来连接适当的FirehoseFactory流数据。

有关JSON多态性的更多信息,请参考下面的网站内容: http://wiki.fasterxml.com/JacksonPolymorphicDeserialization

有关规范文件的更多信息,请参阅以下网站: https://github.com/metamx/druid/wiki/Realtime

现在,我们可以将注意力转向Druid的实现。Firehose是主要必须实现的接口，它提供数据到一个Druid实时服务器。

StormFirehoseFactory类的代码如下:

	@JsonTypeName("storm")
	public class StormFirehoseFactory implements
        FirehoseFactory {
	    private static final StormFirehose FIREHOSE = new StormFirehose();
	
	    @JsonCreator
	    public StormFirehoseFactory() {
	    }
	
	    public static StormFirehose getFirehose() {
	        return FIREHOSE;
	    }
	
	    @Override
	    public Firehose connect() throws IOException {
	        return FIREHOSE;
	    }
	}

工厂实现很简单。在本例中,我们仅仅返回一个静态单例对象。注意,对象是注释@JsonTypeName和@JsonCreator。如上所述在前面的代码中,Jackson是FirehoseFactory对象注册的方式。因此,名称指定为@JsonTypeName必须结合规范文件中指定的类型。

核心的实现是StormFirehose类。在这个类中,有四个关键的方法,我们将一个接一个研究:hasMore(),nextRow()、commit()和sendmessage()。

sendmessage()方法是进入StormFirehose类的入口点。它是有效的Storm和Druid之间的切换点。这个方法的代码如下:

    public synchronized void sendMessages(Long partitionId,
                                          List<FixMessageDto> messages) {
        BLOCKING_QUEUE = new ArrayBlockingQueue<FixMessageDto>(messages.size(), false, messages);
        TRANSACTION_ID = partitionId;
        LOG.info("Beginning commit to Druid. [" +  messages.size() + "] messages, unlocking [START]");
        synchronized (START) {
            START.notify();
        }
        try {
            synchronized (FINISHED) {
                FINISHED.wait();
            }
        } catch (InterruptedException e) {
            LOG.error("Commit to Druid interrupted.");
        }
        LOG.info("Returning control to Storm.");
    }

这个方法是同步的,为了防止并发问题。注意,它不做任何超过消息缓冲区复制到一个队列并通知hasMore()方法来释放一批。然后,它阻塞等待Druid完全消费完此批次。

然后,流流向nextRow()方法,代码如下:

    @Override
    public InputRow nextRow() {
        final Map<String, Object> theMap =
                Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);
        try {
            FixMessageDto message = null;
            message = BLOCKING_QUEUE.poll();
            if (message != null) {
                LOG.info("[" + message.symbol + "] @ [" + message.price + "]");
                theMap.put("symbol", message.symbol);
                theMap.put("price", message.price);
            }
            if (BLOCKING_QUEUE.isEmpty()) {
                STATUS.putInLimbo(TRANSACTION_ID);
                LIMBO_TRANSACTIONS.add(TRANSACTION_ID);
                LOG.info("Batch is fully consumed by Druid. "
                        + "Unlocking [FINISH]");
                synchronized (FINISHED) {
                    FINISHED.notify();
                }
            }
        } catch (Exception e) {
            LOG.error("Error occurred in nextRow.", e);
            System.exit(-1);
        }
        final LinkedList<String> dimensions = new LinkedList<String>();
        dimensions.add("symbol");
        dimensions.add("price");
        return new MapBasedInputRow(System.currentTimeMillis(), dimensions, theMap);
    }

这个方法将从一个队列拉取消息。如果不为空,数据添加到map,把Druid作为MapBasedInputRow传递方法。如果没有剩余的消息在队列中,sendmessage()方法,我们研究了在前面的代码被释放。从Storm的角度来看,批处理完成。Druid现在拥有数据。然而,从系统的角度来看,数据是在limbo装填因为Druid可能没有保存数据到磁盘。我们正处于一个完全丢失数据的风险在发生硬件故障时。

Druid将调用hasMore()方法,如下所示代码:

    @Override
    public boolean hasMore() {
        if (BLOCKING_QUEUE != null
                && !BLOCKING_QUEUE.isEmpty())
            return true;
        try {
            synchronized (START) {
                START.wait();
            }
        } catch (InterruptedException e) {
            LOG.error("hasMore() blocking interrupted!");
        }
        return true;
    }

因为队列是空的,该方法将阻塞,直到sendMessage()被再次调用。

这使得只有一个剩余的问题,commit()方法。这是下面的代码所示:

    @Override
    public Runnable commit() {
        List<Long> limboTransactions = new ArrayList<Long>();
        LIMBO_TRANSACTIONS.drainTo(limboTransactions);
        return new StormCommitRunnable(limboTransactions);
    }

该方法返回Runnable对象,持久化消息后将被Druid调用。尽管在Firehose对象所有其他方法从一个线程调用,从一个不同的线程调用Runnable,因此,必须是线程安全的。出于这个原因,我们的交易从limbo状态复制到一个单独的列表并将其传递到Runnable对象的构造函数。正如你所看到的在下面的代码,运行的事务只会移动到zookeeper完成状态。

	public class StormCommitRunnable implements Runnable {
	    private static final Logger LOG = LoggerFactory.getLogger(StormCommitRunnable.class);
	    private List<Long> partitionIds = null;
	
	    public StormCommitRunnable(List<Long> partitionIds) {
	        this.partitionIds = partitionIds;
	    }
	
	    @Override
	    public void run() {
	        try {
	            StormFirehose.STATUS.complete(partitionIds);
	        } catch (Exception e) {
	            LOG.error("Could not complete transactions.", e);
	        }
	    }
	}

###在zookeeper中实现分区状态

现在,我们已经完成了所有的代码,我们可以看看状态事如何保存在Zookeeper中的。这使系统协调分布式处理,特别是在发生故障时。

利用zookeeper实现持久化partition-processing状态。ZooKeeper是另一个开放源码项目。要了解更多信息,您可以参考 http://zookeeper.apache.org/。

ZooKeeper维护一个节点树。每个节点都有一个相关联的路径,就像文件系统。实现使用zookeeper通过一个名为Curator的框架。更多信息,您可以参考 http://curator.incubator.apache.org/。

当连接到动zookeeper通过curator,您提供一个命名空间。这是顶级节点的应用程序有效的数据存储。在我们的实现中,命名空间是stormdruid。然后,应用程序维护下面三个路径来存储状态信息。

设计的路径对应描述的状态如下:

- /stormdruid/current:对应于当前状态
- /stormdruid/limbo:这对应于limbo状态
- /stormdruid/completed:这对应于完成状态

在我们的实现中,zookeeper的所有分区状态交互通过tDruidPartitionStatus类来完成。这个类的代码如下:

	public class DruidBatchStatus {
	    private static final Logger LOG = LoggerFactory.getLogger(DruidBatchStatus.class);
	    final String COMPLETED_PATH = "completed";
	    final String LIMBO_PATH = "limbo";
	
	    final String CURRENT_PATH = "current";
	    private CuratorFramework curatorFramework;
	
	    public DruidBatchStatus() {
	        try {
	            curatorFramework =
	                    CuratorFrameworkFactory.builder()
	                            .namespace("stormdruid")
	                            .connectString("localhost:2181")
	                            .retryPolicy(new RetryNTimes(1, 1000))
	                            .connectionTimeoutMs(5000)
	                            .build();
	            curatorFramework.start();
	            if (curatorFramework.checkExists().forPath(COMPLETED_PATH) == null) {
	                curatorFramework.create().forPath(COMPLETED_PATH);
	            }
	        } catch (Exception e) {
	            LOG.error("Could not establish connection to Zookeeper", e);
	        }
	    }
	
	    public boolean isCompleted(String paritionId) throws Exception {
	        return (curatorFramework.checkExists().forPath(COMPLETED_PATH + "/" + paritionId) != null);
	    }
	    public boolean isInLimbo(String paritionId) throws Exception {
	        return (curatorFramework.checkExists().forPath(LIMBO_PATH + "/" + paritionId) != null);
	    }
	    public boolean isInProgress(String paritionId) throws Exception {
	        return (curatorFramework.checkExists().forPath(CURRENT_PATH + "/" + paritionId) != null);
	    }
	    public void putInLimbo(Long paritionId) throws Exception {
	        curatorFramework.inTransaction().
	                delete().forPath(CURRENT_PATH + "/" + paritionId)
	                .and().create().forPath(LIMBO_PATH + "/" + paritionId).and().commit();
	    }
	    public void putInProgress(String paritionId) throws Exception {
	        curatorFramework.create().forPath(CURRENT_PATH + "/" + paritionId);
	    }
	}

为了空间,我们只有显示构造函数和相关的limbo状态的方法。在构造函数中,客户机连接到zookeeper并创建三个基础路径在前面描述的代码。然后,它提供了查询方法来测试是否事务在处理中,limbo,或完成。它还提供了在这些状态之间事务转换的方法。

##执行实现

有了上面足够的代码,让我们继续演示!我们开始使用拓扑FinancialAnalyticsTopology类的主要方法开始。为了更好的演示,我们引入随机价格在0到一百之间。(参考发射器代码。)

一旦拓扑启动,您将看到以下输出:

	2014-02-16 09:47:15,479-0500 | INFO [Thread-18]DefaultCoordinator.initializeTransaction(24) | Initializing Transaction [1615]
	2014-02-16 09:47:15,482-0500 | INFO [Thread-22] DruidState.commit(28) | Committing partition [0] of batch [1615]
	2014-02-16 09:47:15,484-0500 | INFO [Thread-22] StormFirehose.sendMessages(82) | Beginning commit to Druid. [7996] messages, unlocking [START]
	2014-02-16 09:47:15,511-0500 | INFO [chief-stockinfo] StormFirehose.nextRow(58) | Batch is fully consumed by Druid. Unlocking [FINISH]
	2014-02-16 09:47:15,511-0500 | INFO [Thread-22] StormFirehose.sendMessages(93) | Returning control to Storm.
	2014-02-16 09:47:15,513-0500 | INFO [Thread-18] DefaultCoordinator.success(30) | Successful Transaction [1615]

你可以从多个维度查询处理。

使用zookeeper客户端,您可以检查事务的状态。看一看下面的清单,它显示了事务/批处理标识符和他们的状态:

	[zk: localhost:2181(CONNECTED) 50] ls /stormdruid/current
	[501-0]
	[zk: localhost:2181(CONNECTED) 51] ls /stormdruid/limbo
	[486-0, 417-0, 421-0, 418-0, 487-0, 485-0, 484-0, 452-0, ...
	[zk: localhost:2181(CONNECTED) 82] ls /stormdruid/completed
	[zk: localhost:2181(CONNECTED) 52] ls /stormdruid/completed
	[59-0, 321-0, 296-0, 357-0, 358-0, 220-0, 355-0,

为了报警和监控,请注意以下几点:

- 如果有多个批处理在当前路径,然后应该发出警报
- 如果有批处理标识符在limbo状态并不是连续的,或大大落后于当前标识符,应该发出警报

清理zookeeper的状态,您可以执行下列代码:

	zk: localhost:2181(CONNECTED) 83] rmr /stormdruid

监控段传播,可以使用MySQL客户端。使用默认的模式,你会发现部分通过选择他们prod_segments表的下面的代码:

	mysql> select * from prod_segments;

##检查分析

现在,我们都在等待那一刻,我们可以看到平均股票价格随着时间的推移,通过使用Druid提供的REST API。使用REST API,它不需要运行完全的Druid集群。您只能查询看到的奇异嵌入式实时的数据节点,但每个节点能够服务请求,这使得测试更加容易。使用curl,您可以发布实时的查询节点使用以下命令:

	curl -sX POST "http://localhost:7070/druid/v2/?pretty=true" -H'content-type: application/json' -d @storm_query

curl的最后一个参数声明引用一个文件,内容将包括POST请求的主体。文件包含以下细节:

	{
		"queryType": "groupBy",
		"dataSource": "stockinfo",
		"granularity": "minute",
		"dimensions": ["symbol"],
		"aggregations":[
			{ "type": "longSum", "fieldName": "orders", "name": "cumulativeCount"},
			{ "type": "doubleSum", "fieldName":	"totalPrice",	"name": "cumulativePrice" }
		],
		"postAggregations":[
		{ "type":"arithmetic",
			"name":"avg_price",
			"fn":"/",
			"fields":[ {"type":"fieldAccess","name":"avgprice","fieldName":"cumulativePrice"},
                 	{"type":"fieldAccess","name":"numrows","fieldName":"cumulativeCount"}]}
			],
		"intervals":["2012-10-01T00:00/2020-01-01T00"]
	}

有两种类型的聚合发生在Druid。有索引时的聚合和查询时的聚合。索引期间发生的聚合在规范文件中定义。如果你还记得,我们有两个聚合在规范文件:

	"aggregators": [
		{ "type": "count", "name": "orders"},
		{ "type": "doubleSum", "fieldName": "price", "name": "totalPrice" }
	],	

我们聚合的事件有两个字段:符号和价格。前面聚合在应用索引时,引入两个附加字段:totalPrice和orders。记得totalPrice是在每个事件的时间段价格之和。订单字段包含在这段时间事件的总数。

然后,当我们执行查询时,Druid适用于第二组聚合基于groupBy声明。在我们的查询中,我们组通过符号的粒度一分钟。然后聚合引入两个新字段:cumulativeCount和cumulativePrice。这些字段包含的前面的聚合总和。

最后,我们介绍一个postaggregation方法来计算平均片的时间。postaggregation方法通过(fn”:“/”)分割两个累积字段产生一个新的avg_price字段。

curl命令的声明对正在运行的服务器导返回如下结果:
	
	[ {
		"version" : "v1",
		"timestamp" : "2013-05-15T22:31:00.000Z",
		"event" : {
			"cumulativePrice" : 3464069.0,
			"symbol" : "MSFT",
			"cumulativeCount" : 69114,
			"avg_price" : 50.12108979367422
		}
	}, {
		"version" : "v1",
		"timestamp" : "2013-05-15T22:31:00.000Z",
		"event" : {
		"cumulativePrice" : 3515855.0,
		"symbol" : "ORCL",
		"cumulativeCount" : 68961,
		"avg_price" : 50.98323690201708
		}
		...
		{
		"version" : "v1",
		"timestamp" : "2013-05-15T22:32:00.000Z",
		"event" : {
			"cumulativePrice" : 1347494.0,
			"symbol" : "ORCL",
			"cumulativeCount" : 26696,
			"avg_price" : 50.47550194785736
		}
		}, {
		"version" : "v1",
		"timestamp" : "2013-05-15T22:32:00.000Z",
		"event" : {
			"cumulativePrice" : 707317.0,
			"symbol" : "SPY",
			"cumulativeCount" : 13453,
			"avg_price" : 52.576897346316805
		}
	} ]

因为我们更新的代码生成随机价格在0和一百之间,意料之外的是,平均大约五十。(呜呼!)

##总结

在这一章,我们获得了更深的Trident API的认识。我们创建了一个状态和StateUpdater接口的直接实现而不是依赖默认实现。具体来说,我们实现这些接口，为了减少事务槽和一个非事务性系统之间的差距,即Druid。虽然不可能建立只有一次语义为非事务性存储,我们建立机制以警惕当系统遇到的问题。表面上,发生故障时我们可以使用一个批处理机制重建任何可疑聚合段。

未来的深入,这将对于Storm和Druid之间建立一个幂等接口是有益的。要做到这一点,我们可以发布一个段为每一批Storm。因为在Druid中段传播是原子,这将给我们一个机制来自动提交每批Druid。此外,批次可以并行处理,提高吞吐量。Druid支持一组不断扩大的查询类型和聚合机制。这是是令人难以置信的强大,Storm和Druid是一个强大的一个结合。