package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
	private LoxClass class_;
	private Map<String, Object> fields = new HashMap<>();
	
	LoxInstance(LoxClass class_) {
		this.class_ = class_;
	}
	
	Object get(Token name) {
		if (this.fields.containsKey(name.lexeme))
			return this.fields.get(name.lexeme);
		
		throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
	}
	
	void set(Token name, Object value) {
		this.fields.put(name.lexeme, value);
	}
	
	@Override
	public String toString() {
		return "<instance of " + this.class_.name + ">";
	}
}
