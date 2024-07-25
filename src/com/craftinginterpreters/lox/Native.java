package com.craftinginterpreters.lox;

abstract class Native implements LoxCallable {
	final String name;
	private final int arity;

	Native(String name, int arity) {
		this.name = name;
		this.arity = arity;
	}

	@Override
	public int arity() {
		return this.arity;
	}

	@Override
	public String toString() {
		return "<native fn " + this.name + ">";
	}
}