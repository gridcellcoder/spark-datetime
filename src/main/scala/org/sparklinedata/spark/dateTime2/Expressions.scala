package org.sparklinedata.spark.dateTime2

import scala.language.implicitConversions
import com.github.nscala_time.time.Imports._
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.catalyst.analysis.FunctionRegistry
import org.apache.spark.sql.catalyst.analysis.FunctionRegistry.FunctionBuilder
import org.apache.spark.sql.catalyst.expressions.codegen.{CodeGenContext, GeneratedExpressionCode}
import org.apache.spark.sql.catalyst.expressions.{Expression,
ExpressionInfo, ImplicitCastInputTypes, UnaryExpression}
import org.apache.spark.sql.types.{DataType, StringType}
import org.apache.spark.unsafe.types.UTF8String
import org.joda.time.DateTime
import org.apache.spark.sql.catalyst.ScalaReflection

case class spklDateTime(child: Expression) extends UnaryExpression with ImplicitCastInputTypes {

  override def inputTypes = Seq(StringType)

  override def dataType: DataType = new SparkDateTimeUDT

  override protected def nullSafeEval(s: Any): Any = {
    Expressions.dateTimeFn(s.asInstanceOf[UTF8String])
  }

  override protected def genCode(ctx: CodeGenContext, ev: GeneratedExpressionCode): String = {
    val e = Expressions.getClass.getName.stripSuffix("$")
    defineCodeGen(ctx, ev, c => s"$e.dateTimeFn($c)")
  }
}

trait ExpressionRegistration {

  def functionRegistry(implicit sqlContext : SQLContext) = {
    import ScalaReflection.{universe => ru}
    val m = ru.runtimeMirror(sqlContext.getClass.getClassLoader)
    val im = m.reflect(sqlContext)
    val fieldFRTerm = ru.typeOf[SQLContext].declaration(ru.newTermName("functionRegistry")).asTerm
    val frField = im.reflectField(fieldFRTerm)
    frField.get.asInstanceOf[FunctionRegistry]
  }

  def registerExpression[T](nm : String)(implicit functionRegistry: FunctionRegistry) : Unit = {
    val t : (String, (ExpressionInfo, FunctionBuilder)) =
      FunctionRegistry.expression[spklDateTime]("dateTimeE")

    functionRegistry.registerFunction(t._1, t._2._1, t._2._2)
  }
}

object Expressions extends ExpressionRegistration {

  def register(implicit sqlContext : SQLContext) : Unit = {
    implicit val FR = functionRegistry
    registerExpression[spklDateTime]("dateTimeE")
  }

  def parseDate(s: String): DateTime = DateTime.parse(s)

  implicit def dateTimeToSpark(dt: DateTime) = SparkDateTime2(dt.getMillis,
    dt.getChronology.getZone.getID)

  implicit def sparkToDateTime(dt: SparkDateTime2) =
    new DateTime(dt.millis).withZone(DateTimeZone.forID(dt.tzId))

  def dateTimeFn(s: UTF8String): Any =
    SparkDateTimeUDT._serialize(dateTimeToSpark(parseDate(s.toString).withZone(DateTimeZone.UTC)))
}