fun putChar(c : char): void = none;
fun getChar(): char = (0 : char);

fun putInt(num : int): void = none;
fun getInt() : int = ({
    num = 0;
    prevC = '0';
    c = getChar();

    #{ Consume until we get a number }#
    while c < '0' | c > '9' do
        prevC = c;
        c = getChar();
    end;

    sign = 0;
    if prevC == '-' then
        sign = 1;
    end;

    #{ Parse number }#
    while c >= '0' & c <= '9' do
        num = num * 10 + ((c : int) - ('0' : int));
        c = getChar();
    end;

    if sign then
        num = -num;
    end;
    num;
} where
    var c : char;
    var prevC : char;
    var num : int;
    var sign : int;
);

fun endl(): void = putChar((10 : char));

fun writeInt(num: int): void = {
    putInt(num);
    endl();
};

fun _privatePrintChar(chr: char): void = {
    putChar(chr);
    endl();
};

fun writeChar(chr: char): void = {
    putChar(chr);
    endl();
};