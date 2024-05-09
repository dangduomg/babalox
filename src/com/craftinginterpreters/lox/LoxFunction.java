package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
  private final Stmt.Function declaration;
  private final Environment closure;

  LoxFunction(Stmt.Function declaration, Environment closure) {
    this.declaration = declaration;
    this.closure = closure;
  }

  @Override
  public int arity() {
    return this.declaration.params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    var environment = new Environment(this.closure);
    for (var i = 0; i < this.arity(); i++) {
      environment.define(this.declaration.params.get(i), arguments.get(i));
    }

    try {
      interpreter.executeBlock(this.declaration.body, environment);
    } catch (Return ret) {
      return ret.value;
    }
    return null;
  }

  @Override
  public String toString() {
    return "<fn " + this.declaration.name.lexeme + ">";
  }
}