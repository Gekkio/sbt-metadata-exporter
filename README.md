sbt-metadata-export-plugin
=================

## Introduction

sbt-metadata-export-plugin is a plugin for [Simple Build Tool](http://code.google.com/p/simple-build-tool/) that exports SBT project metadata into an XML file that can be consumed by an external tool. _This is a proof of concept project that will initially be used for better Eclipse integration for SBT projects_. In theory it could be used for other SBT-related external tool integrations.

## Usage

__This doesn't work at the moment as there are no official releases!__

First, add the maven repository and the plugin declaration to project/plugins/Plugins.scala:

    import sbt._

    class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
      val jawsyMavenReleases = "Jawsy.fi M2 releases" at "http://oss.jawsy.fi/maven2/releases"
      val jrebelPlugin = "fi.jawsy" % "sbt-metadata-export-plugin" % "0.1.0-SNAPSHOT"
    }


Then mix the plugin into your project definition:

_Web projects:_

    import fi.jawsy.sbtplugins.metadata.ExportDefaultWebProject
    import sbt._

    class SomeProject(info: ProjectInfo) extends DefaultWebProject(info) with ExportDefaultWebProject {
    }

## Supported SBT actions

*   export-metadata

    > Exports project metadata in XML format
