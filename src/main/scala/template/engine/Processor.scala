package template.engine

import sbt._
import processor.{Processor, ProcessorResult}
import sbt.processor.BasicProcessor
import net.liftweb.common.{Box, Empty, Failure, Full}
import java.io.File

case class CommandResult(message: String)
trait Command {
  def keyword: String
  def run(arguments: List[String]): CommandResult
}

trait TemplateProcessor {
  
  def templates: List[Template]
  def commands: List[Command] = List(CreateCommand, DeleteCommand, TemplatesCommand, HelpCommand)
  
  def processInput(args: String): Unit = {

    val argsArr = args.split(" ")

    val keyword = argsArr(0)
    val arguments = argsArr.toList - keyword
	
    val result = commands.filter( command => command.keyword == keyword) match {
			case command :: rest => command.run(arguments)
			case Nil => CommandResult("[error] Command is not supported")
		}
		println(result.message)
  }
  
  //# Protected 
  protected def findTemplate(name: String): Box[Template] = templates.filter( _.name == name) match {
      case template :: rest => Full(template) 
      case Nil => Failure("[error] No template with the name %s".format(name))
  }
  
  //#commands
  
  // TODO: Both Create and DeleteCommand are almost identical - refactor slightly
  object CreateCommand extends Command {
    def keyword = "create"
    
    def run(arguments: List[String]): CommandResult = {
      val templateName = arguments(0)
      findTemplate(templateName) match {
        case Full(template) => template.process("create",arguments-arguments(0));
        case Failure(msg,_,_) => CommandResult(msg)
				case Empty => CommandResult("no such template") // TODO: no sure what to do here
      }
    }
  }

  object DeleteCommand extends Command {
    def keyword = "delete"
    def run(arguments: List[String]): CommandResult = {
      val templateName = arguments(0)
      findTemplate(templateName) match {
        case Full(template) => template.process("delete",arguments-arguments(0));
        case Failure(msg,_,_) => CommandResult(msg)
				case Empty => CommandResult("no such template") // TODO: no sure what to do here
      }
    }
  }

  object TemplatesCommand extends Command {
    def keyword = "templates"
    def run(arguments: List[String]): CommandResult = CommandResult("[todo] This should list all templates")
  }

  object HelpCommand extends Command {
    def keyword = "help"
    def run(arguments: List[String]): CommandResult = CommandResult("[todo] This should list all commands")
  }
  
}

// Used to store information about the classpath and other stuff that is different
// for the app if it's running as a processor vs. sbt console
object GlobalConfiguration {
	var scalaCompilerPath = ""
	var scalaLibraryPath = ""
	var scalatePath = ""
	var rootResources = ""
	var runningAsJar = 
		new File(this.getClass.getProtectionDomain.getCodeSource.getLocation.toURI.getPath).getAbsolutePath.contains(".jar")
}

// This is the class you want to extend if you're creating an SBT processor
trait SBTTemplateProcessor extends BasicProcessor with TemplateProcessor {
  
  def apply(project: Project, args: String) = { 
		val scalatePath = { // TODO: Must be a prettier way to do this! 
			val base =  project.info.bootPath.absolutePath
			base + "/scala-2.7.7/sbt-processors/com.sidewayscoding/sbt_template_engine/0.1/scalate-core-1.0-local.jar"
		} 
		GlobalConfiguration.rootResources = ""
		GlobalConfiguration.scalatePath = scalatePath
   	GlobalConfiguration.scalaCompilerPath = project.info.app.scalaProvider.compilerJar.getPath
		GlobalConfiguration.scalaLibraryPath = project.info.app.scalaProvider.libraryJar.getPath
		processInput(args)
  }
}

// This is the class you want to extend if you're creating an stand alone app 
trait StandAloneTemplateProcessor extends TemplateProcessor {
    
  def main(args: Array[String]): Unit = {
    GlobalConfiguration.rootResources = "src/main/resources" 
		processInput( args.mkString(" ") )
  }
}
