package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
  int arity();
  Object call(Interpreter interpreter, List<Object> arguments);

  static abstract class Native implements LoxCallable {
    final String name;
    private final int _arity;

    Native(String name, int arity) {
      this.name = name;
      this._arity = arity;
    }

    @Override
    public int arity() { return this._arity; }

    @Override
    public String toString() { return "<native fn "+this.name+">"; }
  }
}