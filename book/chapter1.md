# 第一章 分布式单词记数
本章，主要介绍使用storm开发分布式流处理应用的基本概念。我们将构建一个统计持续流动的句子中单词个数的简单应用。通过本章的学习，你将了解到设计一个复杂流计算系统所学需要的多种结构，技术和模式。

我们将首先介绍Storm的数据结构，接下来实现一个完全成熟的Storm应用的各个组件。本章结束,你将基本了解Storm计算结构,搭建开发环境,掌握开发和调试storm应用程序的基本技术。

本章包括以下主题:  
·Storm的基本结构——topologies, streams, spouts, and bolts  
·建立Storm的开发环境  
·实现一个基本的单词计数应用程序  
·并行化和容错  
·并行计算任务扩展  

###介绍的Storm拓扑的基本元素——streams, spouts, and bolts

在Storm中,分布式计算的结构被称为一个拓扑，它由流数据,Spouts(流生产者),以及Bolt(操作)组成。Storm拓扑大致类似于批处理作业，例如Hadoop处理系统等。然而,批作业都清楚定义了任务开始和结束点,Strom拓扑确一直运行下去,直到显式地kill或解除部署。

![topology](./pic/1/topology.png)

####Streams

Storm的核心数据结构是元组。元组是一个简单的命名值列表(键-值对),流是一个无界元组序列。如果你熟悉复杂事件处理(CEP),你可以把Storm元组看作是事件。

####Spouts

Spout是storm拓扑的主要数据入口点。Spout像适配器一样连接到一个源的数据,将数据转换为元组,发然后发射出一连串的元组。

正如您了解的,Storm提供了一个简单的API实现Spout。开发一个Spout主要是编写代码从原始源或API消费数据。主要的数据来源包括:  
·web网站或移动应用程序的点击流    
·Twitter或其他社交网络输入  
·传感器输出  
·应用程序日志事件  

因为Spout通常不实现任何特定的业务逻辑,他们常常可以被多个拓扑重用。

####Bolts


Bolts可以被认为是运算操作或函数。它可以任意数量的流作为输入,处理数据,并可选地发出一个或多个流。Bolt可以从Spout或其他bolt订阅流,使它可以形成一个复杂的网络流的转换。

像Spout API一样，Bolts可以执行任何形式的处理,而且bolt的接口简单直接。典型的Bolt执行的功能包括:  
·过滤元组  
·连接和聚合  
·计算  
·数据库读/写  


###介绍的单词计数拓扑数据流

我们的单词计数拓扑(下图中所示)将由一个Spout接着三个bolt组成。
 
![word_count](./pic/1/word_count.jpg)


####Sentence spout


SentenceSpout类只会发出一连串的单值元组，名字为“sentence”和一个字符串值(一个句子),像下面的代码:

    { "sentence":"my dog has fleas" }


为简单起见,我们的数据的来源将是一个不变的句子列表，我们遍历这些句子,发射出每个句子的元组。在真实的应用程序中,一个Spout通常连接到一个动态数据源,如从Twitter API查询得到的微博。

####分词bolt

分割句子bolt将订阅句子spout的元组流。对收到的每个元组,它将查找“句子”对象的值,然后分割成单词,每个单词发射出一个元组:

    { "word" : "my" }
    { "word" : "dog" }
    { "word" : "has" }
    { "word" : "fleas" }

####单词统计bolt


单词统计Spout订阅SplitSentenceBolt类的输出,持续对它收到的特定词记数。每当它收到元组,它将增加与单词相关联计数器,并发出当前这个词和当前记数:

    { "word" : "dog", "count" : 5 }

####报告 bolt

该报告bolt订阅WordCountBolt类的输出并维护一个表包含所有单词和相应的数量,就像WordCountBolt一样。当它收到一个元组,它更新表并将内容打印到控制台。

###实现单词统计拓扑


前面我们已经介绍了基本的Storm概念,接下来我们将开发一个简单的应用程序。现在,我们在本地模式下开发和运行Storm拓扑。Storm的本地模式是在一个JVM实例中模拟Storm集群,便于在本地开发环境或IDE中开发和调试Storm拓扑。在后面的章节中,我们将向您展示如何在本地模式下开发Storm拓扑并部署到完全分布式集群环境中。

####建立开发环境


创建一个新的Storm项目只是把Storm库和其依赖添加到Java类路径中。然而,当您将学完第二章--storm集群配置,你可将Strom拓扑和你编译环境需要特殊的包部署到集群中。因此,强烈建议您使用一个构建管理工具,比如Apache Maven，Gradle或Leinengen。在分布式单词记数的例子中,我们将使用Maven。

首先我们创建一个maven项目:

    $ mvn archetype:create -DgroupId=storm.blueprints
    -DartifactId=Chapter1
    -DpackageName=storm.blueprints.chapter1.v1

接下来, 编辑pom.xml文件并添加Storm依赖:

    <dependency>
    <groupId>org.apache.storm</groupId>
    <artifactId>storm-core</artifactId>
    <version>0.9.1-incubating</version>
    </dependency>


然后,使用以下命令通过构建项目测试Maven配置:

    $ mvn install

####注意
####下载示例代码

如果你已经购买Packt的书，您可以使用你的账户从http://www.packtpub.com下载所有中示例代码文件。如果你在其他地方买的这本书,你可以访问http://www.packtpub.com/并注册，会把文件直接邮件给你。

Maven将下载Storm及其所有依赖项。项目已经建立,我们现在就开始写我们的Storm应用程序。

####实现sentence spout

To keep things simple, our SentenceSpout implementation will simulate a
data source by creating a static list of sentences that gets iterated. Each
sentence is emitted as a single field tuple. The complete spout implementation
is listed in Example 1.1.

为简单起见,我们的SentenceSpout实现模拟数据源创建一个静态的句子迭代列表。每一个发出句子作为一个元组。例子1.1给出完整的Spout实现。

#####Example 1.1: SentenceSpout.java


    public class SentenceSpout extends BaseRichSpout {
        private SpoutOutputCollector collector;
        private String[] sentences = {
            "my dog has fleas",
            "i like cold beverages",
            "the dog ate my homework",
            "don't have a cow man",
            "i don't think i like fleas"
        };
	    private int index = 0;
	
	    public void declareOutputFields(OutputFieldsDeclarer declarer) {
	        declarer.declare(new Fields("sentence"));
	    }
	
	    public void open(Map config, TopologyContext
	            context, SpoutOutputCollector collector) {
	        this.collector = collector;
	    }
	    public void nextTuple() {
	        this.collector.emit(new Values(sentences[index]));
	        index++;
	        if (index >= sentences.length) {
	            index = 0;
	        }
	        Utils.sleep(1);
	    }
    }