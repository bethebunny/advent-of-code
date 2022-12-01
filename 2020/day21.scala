import advent_of_code.data.{dataForDay, sessionID}


object Main extends App {
  val data = dataForDay(21).toSeq
  val ingredientsRE = raw"(.*) \(contains (.*)\)".r
  case class Food(ingredients: Set[String], allergens: Set[String]) {}
  val foods = data.map{
    case ingredientsRE(ingredients, allergens) =>
      Food(ingredients.split(" ").toSet, Option(allergens).getOrElse("").split(", ").toSet)
  }
  val allIngredients = foods.flatMap(_.ingredients).toSet
  val allAllergens = foods.flatMap(_.allergens).toSet
  val allergenIngredients= allAllergens.map(allergen => (allergen,
      foods
        .filter(_.allergens.contains(allergen))
        .map(_.ingredients)
        .foldLeft(foods.flatMap(_.ingredients).toSet)(_ & _)
  )).toMap
  val ingredientsThatCouldBeAllergens= allergenIngredients.values.flatten.toSet
  println(allAllergens)
  println(allergenIngredients)
  println(foods.flatMap(_.ingredients).count(!ingredientsThatCouldBeAllergens.contains(_)))
  /* I'm lazy and did the 2nd part by hand
      dairy -> cdqvp
      eggs -> dglm
      fish -> zhqjs
      peanuts -> rbpg
      sesame -> xvtrfz
      shellfish -> tgmzqjz
      soy -> mfqgx
      wheat -> rffqhl

      cdqvp,dglm,zhqjs,rbpg,xvtrfz,tgmzqjz,mfqgx,rffqhl
    */
}
