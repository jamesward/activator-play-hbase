package controllers

import play.api.mvc.{Action, Controller}
import org.apache.hadoop.hbase.{HColumnDescriptor, HTableDescriptor, HBaseConfiguration}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import play.api.Logger
import play.api.libs.json.Json
import java.util.UUID
import scala.collection.JavaConversions._

object Application extends Controller {

  val barsTableName = "bars"
  val family = Bytes.toBytes("all")
  val qualifier = Bytes.toBytes("json")
  
  lazy val hbaseConfig = {
    val conf = HBaseConfiguration.create()
    val hbaseAdmin = new HBaseAdmin(conf)
    
    // create a table in HBase if it doesn't exist
    if (!hbaseAdmin.tableExists(barsTableName)) {
      val desc = new HTableDescriptor(barsTableName)
      desc.addFamily(new HColumnDescriptor(family))
      hbaseAdmin.createTable(desc)
      Logger.info("bars table created")
    }
    
    // return the HBase config
    conf
  }

  def index = Action {
    // return the server-side generated webpage from app/views/index.scala.html
    Ok(views.html.index("Play Framework + HBase"))
  }
  
  def addBar() = Action(parse.json) { request =>
    // create a new row in the table that contains the JSON sent from the client
    val table = new HTable(hbaseConfig, barsTableName)
    val put = new Put(Bytes.toBytes(UUID.randomUUID().toString))
    put.add(family, qualifier, Bytes.toBytes(request.body.toString()))
    table.put(put)
    table.close()
    Ok
  }
  
  def getBars = Action {
    // query the table and return a JSON list of the bars in the table
    val table = new HTable(hbaseConfig, barsTableName)
    val scan = new Scan()
    scan.addColumn(family, qualifier)
    val scanner = table.getScanner(scan)
    val results = try {
      scanner.toList.map {result =>
        Json.parse(result.getValue(family, qualifier))
      }
    } finally {
      scanner.close()
      table.close()
    }
    Ok(Json.toJson(results))
  }

}
