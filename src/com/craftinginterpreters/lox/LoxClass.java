package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
	final String name;
	final LoxClass super_;
	final Map<String, LoxFunction> methods;

	LoxClass(String name, LoxClass super_, Map<String, LoxFunction> methods) {
		this.name = name;
		this.super_ = super_;
		this.methods = methods;
	}

	@Override
	public Object call(Interpreter intp, List<Object> args) {
		var instance = new LoxInstance(this);
		
		var initializer = this.findMethod("init");
		if (initializer != null)
			initializer.bind(instance).call(intp, args);

		return instance;
	}

	@Override
	public int arity() {
		var initializer = this.findMethod("init");
		if (initializer != null)
			return initializer.arity();
		return 0;
	}

	@Override
	public String toString() {
		return "<class " + this.name + ">";
	}

	LoxFunction findMethod(String name) {
		if (this.methods.containsKey(name))
			return this.methods.get(name);
		else if (this.super_ != null)
			return this.super_.findMethod(name);
		return null;
	}
}
