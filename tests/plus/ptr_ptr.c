import std;
import string;

var ptr_ptr: ^^int;
var ptr: ^int;
var value: int;
var message: string;

fun main(): int = {
    value = 100;
    ptr = ^value;
    ptr_ptr = ^ptr;

    message = "VALUE = 100";
    writeString(message);

    writeInt(value);
    writeInt(ptr^);
    writeInt(ptr_ptr^^);

    value = 200;
    message = "VALUE = 200";
    writeString(message);

    writeInt(value);
    writeInt(ptr^);
    writeInt(ptr_ptr^^);

    ptr^ = 300;
    message = "*ptr = 300";
    writeString(message);

    writeInt(value);
    writeInt(ptr^);
    writeInt(ptr_ptr^^);

    ptr_ptr^^ = 500;
    message = "**ptr_ptr = 500";
    writeString(message);

    writeInt(value);
    writeInt(ptr^);
    writeInt(ptr_ptr^^);

    message = "Print address of value variable";
    writeString(message);

    writeInt((^value : int ));
    writeInt((ptr : int));
    writeInt((ptr_ptr^ : int));

    message = "Print address of value ptr";
    writeString(message);

    writeInt((^ptr : int));
    writeInt((ptr_ptr : int));

    0;
};