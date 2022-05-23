import string;
import std;

var hello: string;
var name: string;
var temp: string;

fun main(): int = {

    hello = "Enter your name:";
    writeString(hello);

    name = getString();
    hello = "Wazzap: ";

    hello = concat(hello, name);
    writeString(hello);
    0;
};