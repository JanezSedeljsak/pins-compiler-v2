import time;
import std;
import string;

var x: int;
var memo: ^int;
var index: int;
var cur_time: int;
var total_time: int;
var str: string;

fun fib(num: int): int = ({
    if num < 2 then
        result = num;
    else
        temp = address(memo, num);
        if temp^ then
            result = temp^;
        else
            result = fib(num - 1) + fib(num - 2);
            temp^ = result;
        end;
    end;

    result;
} where var result: int; var temp: ^int;);

fun main(): int = {
    x = 200;
    memo = make_cache(x*8);

    cur_time = time();
    fib(x);
    index = last_fib_index(memo, x);
    total_time = time() - cur_time;

    str = "Execution took:";
    writeString(str);
    putInt((total_time / 1000) / 1000);
    str = " ms";
    writeString(str);
    0;
};

fun make_cache(size: int): ^int = ({
    temp = (new size : ^int);
    i = 0;

    while i <= size do
        address(temp, i)^ = 0;
        i = i + 1;
    end;

    temp;

} where var temp: ^int; var i: int;);

fun last_fib_index(memo: ^int, size: int): int = ({
    i = 0;
    while i <= size & address(memo, i)^ >= 0 do
        i = i + 1;
    end;

    i - 1;

} where var i: int;);

fun address(ptr: ^int, i: int): ^int = (((ptr: int) + i*8): ^int);

