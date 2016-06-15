package com.testing.utils

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigList, ConfigObject, ConfigValue}

import scala.collection.JavaConversions._
import scala.util.Try

/**
  * @author Roman Maksyutov
  */

trait ImplicitUtils {

	implicit class StringExt(str: String) {

		/**
		  * Convert text like '100 ms' or '100 sec' to time in milliseconds.
		  * If the time unit is not specified then is taken milliseconds by default
		  */
		def toTime: Option[Long] = Try {
			"(\\d+)\\s*(\\w*)".r.findFirstMatchIn(str) match {
				case Some(m) => {
					def parseTimeUnit: String => TimeUnit = {
						case "ms" | "" => TimeUnit.MILLISECONDS
						case "s" | "sec" | "seconds" => TimeUnit.SECONDS
						case "m" | "min" | "minutes" => TimeUnit.MINUTES
						case "h" | "hours" => TimeUnit.MINUTES
					}
					Some(parseTimeUnit(m.group(2)).toMillis(m.group(1).toLong))
				}
				//default execution time
				case None => None
			}

		} getOrElse (throw new IllegalArgumentException("Wrong time format"))
	}

	implicit class ConfigObjectExt(obj: ConfigObject) {

		def apply(key: String): Any = obj.get(key) match {
			case null =>
				null
			case obj: ConfigObject => obj
			case v: ConfigList => v.unwrapped().toList
			case v: ConfigValue => v.unwrapped()
		}

		def option[T](key: String): Option[Any] = apply(key) match {
			case null => None
			case any => Some(any)
		}

		def list[T](key: String): List[T] = obj.get(key) match {
			case v: ConfigList => v.unwrapped().toList.asInstanceOf[List[T]]
			case null => Nil
		}

		def stringList(key: String): Iterable[String] = {
			val config: Config = obj.toConfig
			if (config.hasPath(key))
				config.getStringList(key)
			else
				Nil
		}

		def getTime(key: String)(implicit defaultExecutionTime: Long): Long = Try {
			obj.toConfig.getDuration(key).toMillis
		} getOrElse defaultExecutionTime
	}

}
