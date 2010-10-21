package fi.jawsy.sbtplugins.metadata

import scala.xml._
import sbt._
import sbt.processor._

trait ExportBasicDependencyProject extends MetadataExport {
  override type P <: BasicDependencyProject
  val metadataConfigurations = List("compile", "provided", "runtime", "test")

  override def metadataXml: NodeSeq = {
    import project._
    super.metadataXml ++
    <ivy-configurations>
      { metadataConfigurations.map(new Configuration(_)).flatMap(conf =>
        Some(managedClasspath(conf).get).filter(!_.isEmpty).toSeq.map(cp =>
          <configuration name={ conf.name }>
            { cp.flatMap(entry =>
              <classpathEntry path={ entry.relativePath } />
            )}
          </configuration>
        )
      )}
    </ivy-configurations>
  }
}

trait ExportMavenStyleScalaPaths extends MetadataExport {
  override type P <: MavenStyleScalaPaths

  override def metadataXml: NodeSeq = {
    import project._
    super.metadataXml ++
    <mainCompilePath value={ mainCompilePath.relativePath } />
    <mainJavaSourcePath value={ mainJavaSourcePath.relativePath } />
    <mainResourcesPath value={ mainResourcesPath.relativePath } />
    <mainResourcesOutputPath value={ mainResourcesOutputPath.relativePath } />
    <mainScalaSourcePath value={ mainScalaSourcePath.relativePath } />
    <testCompilePath value={ testCompilePath.relativePath } />
    <testJavaSourcePath value={ testJavaSourcePath.relativePath } />
    <testResourcesPath value={ testResourcesPath.relativePath } />
    <testResourcesOutputPath value={ testResourcesOutputPath.relativePath } />
    <testScalaSourcePath value={ testScalaSourcePath.relativePath } />
  }
}
trait ExportMavenStyleWebScalaPaths extends MetadataExport {
  override type P <: MavenStyleWebScalaPaths

  override def metadataXml: NodeSeq = {
    import project._
    super.metadataXml ++
    <webappPath value={ webappPath.relativePath } />
  }
}

class MetadataExportProcessor extends BasicProcessor {

  implicit def defaultWebProject(p: DefaultWebProject) =
    new ExportProject(p)
    with ExportMavenStyleScalaPaths
    with ExportMavenStyleWebScalaPaths
    with ExportBasicDependencyProject

  implicit def defaultProject(p: DefaultProject) =
    new ExportProject(p)
    with ExportMavenStyleScalaPaths
    with ExportBasicDependencyProject

  implicit def project(p: Project) = new ExportProject(p)

  def apply(project: Project, args: String) {
    project match {
      case p: DefaultWebProject => project.log.info("Detected project as DefaultWebProject"); p.exportMetadata
      case p: DefaultProject => project.log.info("Detected project as DefaultProject"); p.exportMetadata
      case p: ParentProject => {
        project.log.info("Detected project as ParentProject"); p.exportMetadata
        p.dependencies.foreach(apply(_, ""))
      }
      case p: Project => project.log.info("Detected project as Project"); p.exportMetadata
    }
  }
}

class ExportProject[Pr <: Project](val project: Pr) extends MetadataExport {
  override type P = Pr
}

trait MetadataExport {
  val project: P
  type P <: Project

  def metadataXml: NodeSeq = NodeSeq.Empty

  def exportMetadata {
    import project._

    val xml =
      <project name={projectName.value} organization={ projectOrganization.value } version={ projectVersion.value.toString } scalaVersion={ buildScalaVersion } >
        { metadataXml }
      </project>
    val metadataXmlPath = outputPath / "sbt-metadata.xml"
    FileUtilities.touch(metadataXmlPath, log)
    XML.saveFull(metadataXmlPath.relativePath, xml, "UTF-8", true, null)
    log.info("Wrote metadata to " + metadataXmlPath)
  }

}
