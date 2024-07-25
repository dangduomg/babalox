package com.craftinginterpreters.lox;

import java.util.Map;
import java.util.HashMap;

class Environment {
	private final Map<String, Object> values = new HashMap<>();
	private Environment enclosing;

	Environment() {
		this.enclosing = null;
	}

	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	Object get(Token name) {
		if (this.values.containsKey(name.lexeme))
			return this.values.get(name.lexeme);

		if (this.enclosing != null)
			return this.enclosing.get(name);

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	Object getAt(int distance, Token name) {
		return this.getAt(distance, name.lexeme);
	}
	
	Object getAt(int distance, String name) {
		return this.ancestor(distance).values.get(name);
	}
	
	void define(Token name, Object value) {
		this.define(name.lexeme, value);
	}

	void define(String name, Object value) {
		this.values.put(name, value);
	}

	void assign(Token name, Object value) {
		if (this.values.containsKey(name.lexeme)) {
			this.values.put(name.lexeme, value);
			return;
		}

		if (this.enclosing != null) {
			this.enclosing.assign(name, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	void assignAt(int distance, Token name, Object value) {
		var env = this.ancestor(distance);
		if (env.values.containsKey(name.lexeme)) {
			env.values.put(name.lexeme, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}

	Environment ancestor(int distance) {
		var env = this;
		for (int i = 0; i < distance; i++) {
			env = env.enclosing;
		}
		return env;
	}
}