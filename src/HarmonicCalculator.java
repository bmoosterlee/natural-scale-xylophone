public class HarmonicCalculator {

    //Find the next fraction by increasing the denominator and cycling through the numerators
    //we then reduce each fraction using euclid's algorithm.
    //After reducing, we need to check whether the fraction was already used.
    //This is easy however, since any fraction that can be reduced has already been used,
    //since we go through the fractions in order of denominator.
    //Therefore, if euclid's algorithm's solution is 1, we have a new unique fraction.
    //We thus only need to return a boolean for euclid's algorithm.

    //Since we loop through the same fractions each tick, it might be beneficial to store these
    //fractions, and only calculate new ones when we run out of the list.
    //If we are able to calculate the performance strain upon calculating a new fraction,
    //and the performance strain of not having the memory reserved for the last stored fraction,
    //we could throw away fractions that are not being used at the moment.

    //Denominator shows the common period of the two notes, and thus defines the 'sonance' value

}
