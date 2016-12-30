import java.io.{File, FileOutputStream}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.io.StdIn
object WebServer {
  def main(args: Array[String]) {

    implicit val actorSystem = ActorSystem("akka-system")
    implicit val flowMaterializer = ActorMaterializer()

    val interface = "localhost"
    val port = 3000

    import Directives._

    case class Spok(file: String, json: String)

    val route = get {
      pathEndOrSingleSlash {
        complete("Welcome to websocket server")
      }
    } ~
      path("upload") {
        handleWebSocketMessages(echoService)
      }

      val binding = Http().bindAndHandle(route, interface, port)
      println(s"Server is now online at http://$interface:$port\nPress RETURN to stop...")
      StdIn.readLine()

      binding.flatMap(_.unbind()).onComplete(_ => actorSystem.shutdown())
      println("Server is down...")

    }

    implicit val actorSystem = ActorSystem("akka-system")
    implicit val flowMaterializer = ActorMaterializer()


    val echoService: Flow[Message, Message, _] = Flow[Message].mapConcat {

      case BinaryMessage.Strict(msg) => {
        val decoded: Array[Byte] = msg.toArray
        val imgOutFile = new File("/tmp/" + "filename")
        val fileOuputStream = new FileOutputStream(imgOutFile)
        fileOuputStream.write(decoded)
        fileOuputStream.close()
        Nil
      }

      case BinaryMessage.Streamed(stream) => {
        val ss = System.currentTimeMillis()

        stream
          .limit(Int.MaxValue) // Max frames we are willing to wait for
          .completionTimeout(50 seconds) // Max time until last frame
          .runFold(ByteString(""))(_ ++ _) // Merges the frames
          .flatMap { (msg: ByteString) =>

          val decoded: Array[Byte] = msg.toArray
          val imgOutFile = new File("/tmp/" + "filename")
          val fileOuputStream = new FileOutputStream(imgOutFile)
          fileOuputStream.write(decoded)
          fileOuputStream.close()
          Future(Source.single(""))
        }
        Nil
      }

    }




}
