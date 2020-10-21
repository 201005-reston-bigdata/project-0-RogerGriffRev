package AddBook

import java.io.FileWriter

import net.liftweb.json.Extraction.decompose

import scala.collection.mutable.ListBuffer
import scala.io.{Source, StdIn}

import net.liftweb.json._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

import org.mongodb.scala.{MongoClient, MongoCollection, Observable}
import org.mongodb.scala.bson.codecs.Macros._
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.model.Filters




class menu {


  var peopleToPush = new ListBuffer[Person]()
  implicit val formats = DefaultFormats

  val codecRegistries = fromRegistries(fromProviders(classOf[Person]), MongoClient.DEFAULT_CODEC_REGISTRY)
  val client = MongoClient()
  val db = client.getDatabase("addbookpeople").withCodecRegistry(codecRegistries)
  val collection: MongoCollection[Person] = db.getCollection("people")

  /** Nice message to be greeted by */
  def welcomeMen(): Unit ={
    println("Welcome to the Address Book")
  }

  /** The original options on the start menu */
  def menuOptions(): Unit ={
    println("Choose an option from the given list to continue:" +
    "\nImport: allows you to mass import from a .json file(you will be given an option to create single entries after mass importing)" +
      "\nSingle: allows you to create individual entries")
  }

  def getResults[T](obs: Observable[T]): Seq[T] = {
    Await.result(obs.toFuture(), Duration(1, SECONDS))
  }

  def printResults[T](obs: Observable[T]): Unit = {
    getResults(obs).foreach(println(_)).toString

  }


/** create an individual entry for the program */
  def singleCreate(): Unit ={
    println("What is this individual's First Name?")
    val firstName = StdIn.readLine().toString
    println("What is this individual's Last Name?")
    val lastName = StdIn.readLine().toString
    println("What is this individual's Street Address?") //addr can be expanded upon to include like street address, city, state, zipcode,
    val stAddress = StdIn.readLine().toString            // but not necessary for the MVP at the moment
    println("What City does this person live in?")
    val city = StdIn.readLine().toString
    println("What State does this person live in?")
    val state = StdIn.readLine().toString
    println("What is the Zipcode for this person?")
    val zip = StdIn.readLine().toString
    val createdPerson = Person(firstName, lastName, stAddress, city, state, zip)  //take all the info gathered, and make a person object that will then use apply to create a person with an ID
    //println(createdPerson) //debugging code to see if createdPerson was actually created
    peopleToPush += createdPerson
    sOrQuit()
  }

  /** export our peopleToPush to a json file called peopleToAdd.json */
  def exportToJson(): Unit ={
    //val fileWriter = new FileWriter("peopleToAdd.json", true) // causing problems because of how the json is formatted and how append works
                                                                // it is just adding extra brackets which causes the formatting to break
    val fileWriter = new FileWriter("peopleToAdd.json")
    fileWriter.write(prettyRender(decompose(peopleToPush)))     //write the json code that's been rendered to the file opened
    fileWriter.close()
  }

  def pushToDB(): Unit ={
    println(s"There are ${peopleToPush.size} people being pushed to the database")
    //at this point the database is connected and the info is going to start being pushed to it entry by entry from
    for(i <- 0 to (peopleToPush.size - 1)) {
      printResults(collection.insertOne(Person(peopleToPush(i).fname, peopleToPush(i).lname, peopleToPush(i).addr, peopleToPush(i).city, peopleToPush(i).state, peopleToPush(i).zipcode)))
    }
  }

  /** give the user the option to import single entries or quit the program */
  def sOrQuit(): Unit ={
    println("Would you like to create a single entry, or are you finished adding?" +
      "\nMore: add a single entry" +
      "\nExport: export what you have to a json file called peopleToAdd.json" +
      "\nDone: finished adding entries")
    StdIn.readLine().toUpperCase match{ //toUpperCase makes it so that it doesnt matter how they type the word, as long as its spelled right
      case "MORE" => singleCreate()
      case "EXPORT" => exportToJson()
      case "DONE" => {
        println("Pushing all entries to the Database")
      pushToDB()
      }
    }
  }


/** Import a .json file's contents to the list being pushed to the DB */
  def jsonImp(filename: String): Unit ={
    //println("entered jsonImp") //debug code because at the point I wrote it, jsonImp didnt do anything
    val json = Source.fromFile(filename)
    val pToPush = parse(json.mkString)
    json.close()
    val guy: Array[Person] = pToPush.extract[Array[Person]]: Array[Person]
    for(i <- 0 to (guy.size - 1))
    peopleToPush += guy(i)
    //println(pToPush)
    //creating stuff for json file
    sOrQuit()
  }

  def dbEmpty(): Unit ={
  printResults(collection.deleteMany(Filters.exists("fname")))
  }


/** Start the application here, does all the work you need it to */
  def startMenu(): Unit ={
    var keepLoop = true
   // var jsonRes: String = ""

    Thread.sleep(1000)
    welcomeMen()
    do{
      peopleToPush.clear()    // clear out the buffer at the start of each loop so what you get from the last loop doesnt carry over and duplicate
      menuOptions()
      StdIn.readLine().toUpperCase match { //toUpperCase makes it so that it doesnt matter how they type the word, as long as its spelled right
        case "IMPORT" => {
          println("Input the name of the file being imported")
          val f = StdIn.readLine() //getting the name of the file. Lazy coding to do it separately, but it works
          jsonImp(f)
        }
        case "SINGLE" => {
          println("You've chosen to enter in an individual")
          singleCreate()
        }
       case "DELETE" => { //hidden option that will be commented out in final product, used for debugging
         println("Emptying the Database")
         dbEmpty()
       }
      }
      /*for(i <- 0 to (peopleToPush.size - 1)){  //debug code to see who is being pushed to the database
        println(peopleToPush(i))
      }

       */


      println("Anything left to do? Yes or No") //allows user to re-loop through the program
      StdIn.readLine().toUpperCase() match {
        case "YES" => keepLoop = true
        case "NO"  => {
          println("Exiting Address Book.\n Goodbye!")
          keepLoop = false
        }
      }


    }while(keepLoop)
    client.close()
  }

}
