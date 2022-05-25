import std;
import math;

var i: int;
var count: int;
var amount: int;

fun main() : int = {
    count = 0;
    amount = 10000;

    for i = 0; i < amount; i = i + 1; do
        count = count + guess();
    end;

    putInt(count / amount);
    putChar(',');
    writeInt(count % amount);
    0;
};

fun guess(): int = {
    ({
        charToCheck = ( random(65, 15) : char );
        tries = 1;
        inp = ( random(65, 15) : char );
        while inp != charToCheck do {
            inp = ( random(65, 15) : char );
            tries = tries + 1;
            none;
        };
        end;
        tries;
    } where 
        var inp : char; 
        var tries : int; 
        var charToCheck: char;
    ); 
};