package AddBook

import org.mongodb.scala.bson.ObjectId

/** This will be the entries that go in our address book. A default name and address of unconfirmed will be given if user doesn't
 * give a name or address
 */
case class Person(_id: ObjectId, var fname: String, var lname: String , var addr: String, var city: String, var state: String, var zipcode: String) {

  /** making it possible to print a person */
  override def toString() = {
    s"Name: ${fname} ${lname} lives at address: ${addr}, ${city}, ${state}, ${zipcode}"
  }

}
  object Person {
    def apply(fname: String, lname: String, add: String, city: String, state: String,  zipcode: String): Person = Person(new ObjectId, fname, lname, add, city, state, zipcode)

  }






