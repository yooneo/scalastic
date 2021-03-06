package org.elasticsearch.test.integration.indices.template

import org.elasticsearch.index.query.QueryBuilders._
import org.elasticsearch.indices._
import scalastic.elasticsearch._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner]) 
class SimpleIndexTemplateTests extends IndexerBasedTest {

  test("simpleIndexTemplateTests") {
    clean()
    indexer.putTemplate(
      "template_1", template = Some("te*"), order = Some(0),
      mappings = Map("type1" -> """{"type1": {"properties": {"field1": {"type": "string", "store": "yes"}, "field2": {"type": "string", "store": "yes", "index": "not_analyzed"}}}}"""))
    try {
      indexer.putTemplate(
        "template_2", template = Some("test*"), create = Some(true), order = Some(1),
        mappings = Map("type1" -> """{"type1": {"properties": {"field2": {"type": "string", "store": "no"}}}}"""))
      //fail //fixme: should fail recognizing it already exists
    } catch {
      case e: IndexTemplateAlreadyExistsException =>
      case e: Exception => fail()
    }
    indexer.index("test_index", "type1", "1", """{"field1": "value1", "field2": "value 2"}""", refresh = Some(true))
    indexer.refresh()
    indexer.waitForGreenStatus()
    var response = indexer.search(Seq("test_index"), query = termQuery("field1", "value1"), fields = Seq("field1", "field2"))
    if (response.getFailedShards > 0) {
      //println("failed search " + response.getShardFailures.mkString(","))
    }
    response.getFailedShards should be === (0)
    response.getHits.totalHits should be === (1)
    response.getHits.getHits.length should be === (1)
    response.getHits.getAt(0).field("field1").value().toString should be === ("value1")
    response.getHits.getAt(0).field("field2").value().toString should be === ("value 2")
    indexer.index("text_index", "type1", "1", """{"field1": "value1", "field2": "value 2"}""", refresh = Some(true))
    indexer.refresh()
    indexer.waitForGreenStatus()
    response = indexer.search(Seq("text_index"), query = termQuery("field1", "value1"), fields = Seq("field1", "field2"))
    if (response.getFailedShards > 0) {
      //logger.warn("failed search " + response.shardFailures.mkString(","))
    }
    response.getFailedShards should be === (0)
    response.getHits.totalHits should be === (1)
    response.getHits.getHits.length should be === (1)
    response.getHits.getAt(0).field("field1").value().toString should be === ("value1")
    response.getHits.getAt(0).field("field2").value().toString should be === ("value 2")
  }

  private def clean() {
    for (each <- Seq("template_1", "template_2")) {
      try {
        indexer.deleteTemplate(each)
      } catch {
        case e: Exception =>
      }
    }
  }
}
