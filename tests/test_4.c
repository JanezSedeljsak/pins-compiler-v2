var ptr: ^int;
var num: int;

fun add(a: int, b: int): int = a + b;

fun main(): int = {
  num = 10;
  ptr = ^num;

  num = -500 + num;
  ptr^;
};