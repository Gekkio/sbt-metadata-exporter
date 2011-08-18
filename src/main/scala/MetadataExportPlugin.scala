package fi.jawsy.sbtplugins.metadata

import sbt._
import sbt.CommandSupport.logger
import sbt.complete.Parsers._
import scala.xml._

object MetadataExportPlugin extends Plugin {
  lazy val metadata = TaskKey[Unit]("metadata")

  override def settings = Seq(Keys.commands += metadataExport)

  trait ToXml[T] {
    def apply(value: T): NodeSeq
  }

  object ToXml {
    def apply[T](f: (T) => NodeSeq) = new ToXml[T] {
      def apply(value: T) = f(value)
    }
  }

  implicit val string2xml = ToXml[String](Text(_))
  implicit val file2xml = ToXml[File](x =>
      <file path={ x.getAbsolutePath } />
  )
  implicit val module2xml = ToXml[ModuleID](x =>
      <module name={ x.name} organization={ x.organization } version={ x.revision} />
  )
  implicit val artifact2Xml = ToXml[Artifact](x =>
      <artifact name={ x.name } type={ x.`type` } extension={ x.extension } classifier={ x.classifier.getOrElse("") } />
  )

  def toXml[T](value: T)(implicit xmlConv: ToXml[T]) = xmlConv(value)
  def toXml[T](value: Option[T])(implicit xmlConv: ToXml[T]) = value.map(xmlConv.apply).getOrElse(NodeSeq.Empty)

  private val metadataExport =
    Command.command("metadata") { state =>

      val structure = Project extract state structure

      (for (ref <- structure.allProjectRefs) {

        def task[A](key: ScopedKey[Task[A]]) = EvaluateTask.evaluateTask(structure, key, state, ref, false,
          EvaluateTask.SystemProcessors)
  
        def setting[A](key: SettingKey[A], conf: Configuration = Configurations.Compile) = {
          key in (ref, conf) get structure.data
        }

        val name = setting(Keys.name)
        val module = setting(Keys.projectID)
        val scalaLibrary = setting(Keys.scalaInstance).map(_.libraryJar)
        val scalaVersion = setting(Keys.scalaVersion)
        val sbtVersion = setting(Keys.sbtVersion)
        val baseDirectory = setting(Keys.baseDirectory).map(_.getAbsolutePath)

        def seqSetting[T](conf: Configuration = Configurations.Compile, keys: SettingKey[Seq[T]]*) = 
          for (key <- keys;
               value <- setting(key, conf).toList;
               element <- value) yield element

        val sourceDirs = seqSetting(Configurations.Compile, Keys.unmanagedSourceDirectories, Keys.managedSourceDirectories)
        val resourceDirs = seqSetting(Configurations.Compile, Keys.unmanagedResourceDirectories, Keys.managedResourceDirectories)

        val testSourceDirs = seqSetting(Configurations.Test, Keys.unmanagedSourceDirectories,
          Keys.managedSourceDirectories)
        val testResourceDirs = seqSetting(Configurations.Test, Keys.unmanagedResourceDirectories,
          Keys.managedResourceDirectories)

        def toCompileDirXml(dirs: Seq[File], output: Option[File]) =
          for (dir <- dirs) yield
          <compileDirectory src={ dir.getAbsolutePath } output={ toXml(output.map(_.getAbsolutePath)) } />

        def readCompileDirs(confs: Seq[Configuration], keys: SettingKey[Seq[File]]*) =
          (for (conf <- confs) yield {
            toCompileDirXml(seqSetting(conf, keys: _*), setting(Keys.classDirectory, conf))
          }).flatten

        val configurations = List(Configurations.Compile, Configurations.Test)

        def readClasspath(key: TaskKey[Keys.Classpath], conf: Configuration) =
          for(result <- task(key in conf).toSeq;
              classpaths <- (result.toEither.right.toSeq);
              entry <- classpaths;
              file = entry.data if !scalaLibrary.exists(_ == file);
              artifact = entry.metadata.get(Keys.artifact.key);
              module = entry.metadata.get(Keys.moduleID.key)) yield
              <classpathEntry path={ file.getAbsolutePath }>
              { toXml(artifact) }
              { toXml(module) }
              </classpathEntry>

        val xml =
          <project metadataVersion="2" name={ toXml(name) } scalaVersion={ toXml(scalaVersion) } baseDirectory={
          toXml(baseDirectory) } sbtVersion={ toXml(sbtVersion) }>
            { toXml(module) }
            <sourceDirectories>
              { readCompileDirs(configurations, Keys.unmanagedSourceDirectories, Keys.managedSourceDirectories) }
            </sourceDirectories>
            <resourceDirectories>
              { readCompileDirs(configurations, Keys.unmanagedResourceDirectories, Keys.managedResourceDirectories) }
            </resourceDirectories>
            <projectDependencies>
              {
                for(result <- task(Keys.projectDependencies in Configurations.Compile).toSeq;
                    deps <- result.toEither.right.toSeq;
                    dep <- deps) yield toXml(dep)
              }
            </projectDependencies>
            <externalClasspath>
              <compile>
                { readClasspath(Keys.externalDependencyClasspath, Configurations.Compile) }
              </compile>
              <test>
                { readClasspath(Keys.externalDependencyClasspath, Configurations.Test) }
              </test>
            </externalClasspath>
          </project>

        for (target <- setting(Keys.crossTarget)) {
//          val printer = new PrettyPrinter(160, 2)
//          println(printer.format(xml))
          val xmlFile = new File(target, "sbt-metadata.xml")
          XML.save(xmlFile.getAbsolutePath, xml, "UTF-8", true)
          logger(state).info("Wrote metadata to " + xmlFile)
        }
      })
      state
    }

}
