import java.io.{File, FileWriter}
import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import org.apache.commons.lang3.StringUtils
import org.codehaus.jettison.json.JSONObject
import org.json4s.JsonAST.{JInt, JString}
import org.json4s.{DefaultFormats, JValue}
import org.json4s.jackson.Json

import scala.util.control.Breaks._
import scala.io.Source

/**
  * Jvalue to String
  */
object Get {
  def string(value: JValue): String = {
    val JString(result) = value
    result
  }

  def int(value: JValue): BigInt = {
    val JInt(result) = value
    result
  }
}

object JsonUtil {
  def hasObject(json: JSONObject, value: String): Boolean = {
    try {
      json.getJSONObject(value)
      true
    } catch {
      case e: Exception => {

      }
        false
    }
  }

  def getNestedObjectValue(json: JSONObject, field: String, key: String): String = {
    try {
      return json.getJSONObject(field).getString(key)
    } catch {
      case e: Exception => {

      }
        return null
    }
  }
}

class TweetNormalizer {

  private val candidates: Map[String, Set[String]] = Map(
    "arthaud" -> Set("n_arthaud", "Arthaud", "LutteOuvrière", "LO"),
    "asselineau" -> Set("UPR_Asselineau", "JeVoteAsselineau"),
    "cheminade" -> Set("JCheminade", "Cheminade2017", "CHEMINADE", "JeVoteCheminade", "JacquesCheminade"),
    "dupont aignan" -> Set("dupontaignan", "DupontAignan", "JeVoteDupontAignan"),
    "fillon" -> Set("FrancoisFillon", "TousFillon", "Fillon2017_fr","Fillon", "Fillon2017", "FF2017", "FillonGate"),
    "hamon" -> Set("benoithamon", "Hamon2017", "Hamon2022"),
    "lassalle" -> Set("jeanlassalle", "JeVoteLassalle"),
    "le pen" -> Set("MLP_officiel", "Marine2017", "LePen", "MLP", "MLPTF1"),
    "macron" -> Set("EmmanuelMacron", "Macron", "EM", "EnMarche", "JeVoteMacron", "Macron2017", "MacronLeak", "MacronGate"),
    "melenchon" -> Set("JLMelenchon‏","Melenchon", "JLM"),
    "poutou" -> Set("PhilippePoutou", "JeVotePoutou", "Poutou2017", "NPA")
  )

  private val presidentialElections: Set[String] = Set("Presidentielle", "PCF", "Groupe_PRG_CI", "PRG", "GauchePS", "PS", "MoDem", "LiberalUMP", "UMP", "MPF", "FN_officiel", "FN", "FNGate", "lesRépublicains", "LR")

  private val TweetFrSent = new TweetFrSentiment
  TweetFrSent.init()

  // private val hashTagExp = """\B#\w*[a-zA-Z]+\w*""".r
  // private val mentionExp = """\B@\w*[a-zA-Z]+\w*""".r

  private def getCandidate(text: String): String = {
    val words = text.replace("#", "").replace("@", "").toLowerCase.split(" +").toSeq.map(w => StringUtils.stripAccents(w))
    val candidatesFound = words.filter(w => candidates.exists(c => c._1 == w || c._2.contains(w))).distinct

    if (candidatesFound.size != 1) {
      // ignore tweet
      return null
    }

    return candidatesFound.head
  }

  private def concernsPolitics(text: String): Boolean = {
    val words = text.replace("#", "").replace("@", "").toLowerCase.split(" +").toSeq.map(w => StringUtils.stripAccents(w).toLowerCase)
    return words.exists(w => presidentialElections.contains(w)) || words.exists(w => candidates.exists(c => c._1 == w || c._2.contains(w)))
  }

  private def getOutputFile(text: String, createdAt: String): String = {
    val simpleDateFormat: SimpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);
    val date: Date = simpleDateFormat.parse(createdAt);
    val outputFilename = "./output/" + new SimpleDateFormat("yyyy-MM-dd").format(date)

    if (this.concernsPolitics(text)) {
      return outputFilename + ".politics.json"
    } else {
      return outputFilename + ".json"
    }
  }

  def transform(inputFile: String): Unit = {
    val filename = "./input/" + inputFile

    // Check if input file exists
    if (!(new File(filename).exists())) {
      throw new Exception("Input file does not exist: " + filename);
    }

    val linesLength = Source.fromFile(filename).getLines().size
    var lineNumber = 0
    var lineError = 0

    println("######")
    println("Extracting " + linesLength + " tweets from " + filename)
    println("######")

    var currentLine: String = ""

    for (line <- Source.fromFile(filename).getLines()) {
      breakable {
        currentLine = line
        lineNumber += 1

        // remove comma as first char
        if (currentLine.length > 0 && currentLine.charAt(0).toString == ",") {
          currentLine = currentLine.substring(1)
        }
        // remove comma as last char
        if (currentLine.length > 0 && currentLine.charAt(currentLine.length - 1).toString == ",") {
          currentLine = currentLine.substring(0, currentLine.length - 1)
        }

        try {
          val json = new JSONObject(currentLine);
          val tweet = collection.mutable.Map[String, Any]()

          // add primary data
          tweet("created_at") = json.getString("created_at")


          if (this.concernsPolitics(json.getString("text").toString())){
            tweet("id_str") = json.getString("id_str")

            tweet("text") = json.getString("text")
            tweet("retweet_count") = json.getInt("retweet_count")
            tweet("favorite_count") = json.getInt("favorite_count")
            tweet("lang") = json.getString("lang")
            tweet("retweeted") = json.getBoolean("retweeted")
            tweet("favorited") = json.getBoolean("favorited")

            // user
            tweet("user_id") = JsonUtil.getNestedObjectValue(json, "user", "id")
            tweet("user_location") = JsonUtil.getNestedObjectValue(json, "user", "location")
            tweet("user_statuses_count") = JsonUtil.getNestedObjectValue(json, "user", "statuses_count")
            tweet("user_created_at") = JsonUtil.getNestedObjectValue(json, "user", "created_at")
            tweet("user_lang") = JsonUtil.getNestedObjectValue(json, "user", "lang")

            // place
            tweet("country_code") = JsonUtil.getNestedObjectValue(json, "place", "country_code")
            tweet("place_name") = JsonUtil.getNestedObjectValue(json, "place", "name")
            tweet("candidate") = getCandidate(json.getString("text").toString())
            tweet("sentiment") = TweetFrSent.getSentiment(json.getString("text").toString())
            // append in correct file according to the tweet's date
            var write = Json(DefaultFormats).write(tweet).toString
            val outputFile = this.getOutputFile(tweet("text").toString, tweet("created_at").toString)

            ////#########
            // append to new line only if the file contains at least 1 tweet
            if ((new File(outputFile).exists()) && Source.fromFile(outputFile).getLines().length > 0) {
              write = "\n" + write
            }
            val fw = new FileWriter(outputFile, true)
            fw.write(write)
            fw.close()
          }

        } catch {
          case e: Exception => {
            println("WARNING: " + e.getLocalizedMessage + " " + e.getCause + " " + e.getMessage)
          }
            lineError += 1
            break
        }
      }
    }

    // at the end
    println(lineError + " " + "tweets have not been extracted (" + ((lineError.toFloat/linesLength.toFloat) * 100) + "% of failure).")
  }
}
