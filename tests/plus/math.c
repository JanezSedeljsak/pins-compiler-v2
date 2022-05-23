import std;
import math;
import string;

var str: string;

fun main(): int = {
    str = "SQRT (49, 101):";
    writeString(str);
    writeInt(sqrt(49));
    writeInt(sqrt(101));

    str = "LOG (100, 1000):";
    writeString(str);
    writeInt(log(100));
    writeInt(log(1000));
    
    str = "RANDOM (5, 10):";
    writeString(str);
    writeInt(random(5,100));
    writeInt(random(10,1000));

    str = "POW: (5,3), (2,0)";
    writeString(str);
    writeInt(pow(5,3));
    writeInt(pow(2,0));

    0;
};