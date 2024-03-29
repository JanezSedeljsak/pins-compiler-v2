var x: int;
var memo: ^int;

fun fib(num: int): int = ({
    if num < 2 then
        result = num;
    else
        temp = address(memo, num);
        if temp^ != -1 then
            result = temp^;
        else
            result = fib(num - 1) + fib(num - 2);
            temp^ = result;
        end;
    end;

    result;
} where var result: int; var temp: ^int;);

fun main(): int = {
    x = 80;
    memo = make_cache(x*8);
    fib(x);
};

fun make_cache(size: int): ^int = ({
    temp = (new size : ^int);
    i = 0;

    while i <= size do
        address(temp, i)^ = -1;
        i = i + 1;
    end;

    temp;

} where var temp: ^int; var i: int;);

fun address(ptr: ^int, i: int): ^int = (((ptr: int) + i*8): ^int);