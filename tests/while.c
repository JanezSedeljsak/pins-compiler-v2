fun putChar(c : char) : void = none;

fun main(): int = {
    num = 0;
    while (num: char) <= 'Z' do
        arr[num] = (num + 65 : char);
        num = num + 1;
    end;

    num = 0;
    while (num + 65 : char) < 'Z' do
        if num > 5 then
            putChar(arr[num]);
            putChar((10 : char));
        end;
        num = num + 1;
    end;

    0;
};

var num: int;
var arr: [30]char;