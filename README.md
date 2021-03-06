# Spark DateTime Library

A library for exposing dateTime functions from the [joda time library](http://www.joda.org/joda-time/) as SQL 
functions. Also provide a dsl for dateTime catylst expressions; this utilizes the 
[scala wrapper library](https://github.com/nscala-time/nscala-time nscala). 


[![Build Status](https://travis-ci.org/SparklineData/spark-datetime.svg?branch=master)](https://travis-ci.org/SparklineData/spark-datetime)

## Requirements

This library requires Spark 1.4+

## Linking
You can link against this library in your program at the following coordiates:

```
groupId: org.sparklinedata
artifactId: spark-datetime_2.10
version: 0.0.1
```

## Using with Spark shell
This package can be added to  Spark using the `--jars` command line option.  For example, to include it when starting the spark shell:

```
$ bin/spark-shell --packages org.sparklinedata:spark-datetime_2.10:0.0.1
```

## Features
* A set of functions from the joda library to operate on dates.
  *  `field access`: all functions in the [DateTime class](http://www.joda.org/joda-time/apidocs/org/joda/time/DateTime.html) are 
available as sql functions. The first argument is the DateTime object on which the function is to be applied.
  *  `construction`: functions are available to convert a String or a epoch value to DateTime
  *  `comparison` functions available to compare dates (=, <, <=, >, >=), also compare against __now__.
  * `arithmetic`: functions available to add/subtract [Period](http://www.joda.org/joda-time/apidocs/org/joda/time/Period.html)
from dates.
  * `intervals`: functions available to construct [Intervals](http://www.joda.org/joda-time/apidocs/org/joda/time/Interval.html)
  and compare(contains, overlaps, abuts, gap) intervals and dateTimes. 
* A _dsl_ for dateTime catylst expressions.
* A _StringContext_ to embed date expressions in SQL statements.

### Function naming convention
* getter functions on the [DateTime class](http://www.joda.org/joda-time/apidocs/org/joda/time/DateTime.html) are exposed with the same
name, in camelCase. So _getYear_ is exposed as _year_, _getMonthOfYear_ is exposed as _monthOfYear_ etc.

### SQL API
Assume you have a table _input_ with a string column called _dt_

```sql
select dt, dateTime(dt), dayOfWeek(dateTime(dt)), dayOfWeekName(dateTime(dt)), dayOfWeekName(dateTimeWithTZ(dt)) 
from input
```

### Date Expressions using the DSL

#### A basic example
```scala
import com.github.nscala_time.time.Imports._
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.sparklinedata.spark.dateTime.dsl.expressions._
import org.sparklinedata.spark.dateTime.Functions


// register all functions 
Functions.register(sqlContext)

val dT = dateTime('dt)
val dOW = dateTime('dt) dayOfWeek
val dOWNm = dateTime('dt) dayOfWeekName
val dOWNm2 = dateTimeWithTZ('dt) dayOfWeekName
val dTFixed = dateTime("2015-05-22T08:52:41.903-07:00")

val t = sql(date"select dt, $dT, $dOW, $dOWNm, $dOWNm2, $dTFixed," +
      " dateTime(\"2015-05-22T08:52:41.903-07:00\") from input")
```

#### An example about periods
```scala
import com.github.nscala_time.time.Imports._
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.sparklinedata.spark.dateTime.dsl.expressions._

val dT = dateTime('dt)
val dT1 = dateTime('dt) + 3.months
val dT2 = dateTime('dt) - 3.months
val dT3 = dateTime('dt) + 12.week
val cE = dateTime('dt) + 3.months > (dateTime('dt) + 12.week)

val t = sql(date"select dt, $dT, $dT1, $dT2, $dT3, $cE from input")
```

#### Weekend filter example
```scala
val filter : Expression = ((dateTime('dt) dayOfWeekName) === "Saturday") ||
      ((dateTime('dt) dayOfWeekName) === "Sunday")

val t = sql(date"select dt from input where $filter")
```

#### Group By example
```scala
val dayOfWeek: Expression = dateTime('dt) dayOfWeekName

val t = sql(date"select $dayOfWeek, count(*) from input group by $dayOfWeek")
```

#### Interval example
```scala
val i1 = END_DATE - 15.day to END_DATE - 10.day

val isBefore = i1 isBeforeE dateTime('dt)
val isAfter = i1 isAfterE dateTime('dt)
val i2 = dateTime('dt) to (dateTime('dt) + 5.days)
val overlapsE = i1 overlapsE i2
val abutsE = i1 abutsE i2

val t = sql(date"select dt, $isBefore, $isAfter, $overlapsE, $abutsE from input")
```

#### Time Bucketing
Use this feature to bucket dates into given Periods. For e.g. 8.hours, 30.mins, 2.days etc.

The following example buckets rows into 3 day periods.  The *bucket* function on a DateExpression
takes an _origin_ date and a Period specification. The Period is an 
[iso8061 specification for period.](http://joda-time.sourceforge.net/apidocs/org/joda/time/format/ISOPeriodFormat.html#standard\(\))

```scala
val start = dateTime("2015-06-23T17:27:43.769-07:00")
val dT = dateTime('dt)
val timeBucket = dateTime('dt) bucket(start, 3.days)

val t = sql(date"select dt, $dT, $timeBucket from input")
```

Or the direct sql for the above query is:
```sql
select dt, dateTime(`dt`), 
      timeBucket(dateTime(`dt`),dateTime("2015-05-24T17:27:43.769-07:00"),period("P3D")) 
from input
```

## Building From Source
This library is built with [SBT](http://www.scala-sbt.org/0.13/docs/Command-Line-Reference.html), which is 
automatically downloaded by the included shell script. 
To build a JAR file simply run `build/sbt package` from the project root. 
The build configuration includes support for both Scala 2.10 and 2.11.
