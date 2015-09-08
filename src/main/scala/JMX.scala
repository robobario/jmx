import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanParameterInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import scala.collection.JavaConversions._

object JMX {
  def main(args: Array[String]) {
    if(args.size == 0){
      usage()
      System.exit(1)  
    }
    val jmxc = JMXConnectorFactory.connect(new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:11080/jmxrmi"));
      val connection = jmxc.getMBeanServerConnection();
      args(0) match {
        case "-l" => list(connection)
        case "-r" => run(connection, args)
        case _ => usage() 
      }
  }

  def usage(): Unit = {
    println("nope. use -l or -r")
  }

  def run(conn: MBeanServerConnection, args: Array[String]): Unit = {
    io.Source.stdin.getLines.foreach(ln => execute(conn, ln))
  }

  def execute(conn: MBeanServerConnection, ln: String): Unit = {
    val parts = ln.split(" ")
    val name = new ObjectName(parts(0))
    val obj = conn.getMBeanInfo(name)
    val op = obj.getOperations().find(op => op.getName == parts(1)) match {
      case Some(op: MBeanOperationInfo) => executeOp(name ,op, conn)
      case _ => println("no such operation!")
    }
  }

  def executeOp(name: ObjectName, op: MBeanOperationInfo, conn: MBeanServerConnection): Unit = {
    val args = op.getSignature().map(param => getParam(param))
    conn.invoke(name, op.getName, args, op.getSignature().map(param => param.getType))
  }

  def getParam(param: MBeanParameterInfo): Object = {
     val arg = readLine("enter value for " + param.getName + ":")
     param.getType match {
       case "int" => arg.toInt:java.lang.Integer
       case _ => "shittt"
     }
  }

  def list(connection: MBeanServerConnection): Unit = {
    val objectNames = connection.queryNames(new ObjectName("Adscale:*"), null);
    objectNames.foreach(name => printAllMethods(name, connection));
  }

  def printAllMethods(name: ObjectName, connection: MBeanServerConnection): Unit = {
    try {
      connection.getMBeanInfo(name).getOperations().map(op => name.getCanonicalName()+ " " + op.getName()).foreach(println);
    }catch {
      case e: Exception => throw new RuntimeException(e);
    }
  }
}
