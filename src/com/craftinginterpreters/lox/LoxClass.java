package com.craftinginterpreters.lox;

import java.util.List;

class LoxClass implements LoxCallable {
	final String name;
	
	LoxClass(String name) {
		this.name = name;
	}
	
	@Override
	public Object call(Interpreter intp, List<Object> args) {
		var instance = new LoxInstance(this);
		return instance;
	}
	
	@Override
	public int arity() {
		return 0;
	}
	
	@Override
	public String toString() {
		return "<class " + this.name + ">";
	}
}
