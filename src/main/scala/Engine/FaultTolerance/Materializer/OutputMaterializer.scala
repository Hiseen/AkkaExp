package Engine.FaultTolerance.Materializer

import Engine.Common.AmberTag.LayerTag
import Engine.Common.AmberTuple.Tuple
import Engine.Common.TupleProcessor
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import java.io.{BufferedWriter, File, FileWriter}
import java.net.URI

class OutputMaterializer(val outputPath:String, val remoteHDFS:String = null) extends TupleProcessor {

  var writer:BufferedWriter = _

  override def accept(tuple: Tuple): Unit = {
    writer.write(tuple.mkString("|"))
  }

  override def onUpstreamChanged(from: LayerTag): Unit = {

  }

  override def noMore(): Unit = {
    writer.close()
    if(remoteHDFS != null){
        val fs = FileSystem.get(new URI(remoteHDFS),new Configuration())
        fs.copyFromLocalFile(new Path(outputPath),new Path(outputPath))
        fs.close()
      }
  }

  override def initialize(): Unit = {
    val file = new File(outputPath)
    file.mkdirs() // If the directory containing the file and/or its parent(s) does not exist
    file.createNewFile()
    writer = new BufferedWriter(new FileWriter(file))
  }

  override def hasNext: Boolean = false

  override def next(): Tuple = ???

  override def dispose(): Unit = {
    writer.close()
  }

  override def onUpstreamExhausted(from: LayerTag): Unit = {

  }
}
