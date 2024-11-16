package com.craftinginterpreters.lox;

import java.util.List;

class LoxFunction implements LoxCallable {
	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;
	private final LoxClass class_;

	LoxFunction(
		Stmt.Function declaration,
		Environment closure,
		boolean isInitializer,
		LoxClass class_
	) {
		this.declaration = declaration;
		this.closure = closure;
		this.isInitializer = isInitializer;
		this.class_ = class_;
	}

	@Override
	public int arity() {
		return this.declaration.params.size();
	}

	@Override
	public Object call(Interpreter intp, List<Object> args) {
		var environment = new Environment(this.closure);
		for (var i = 0; i < this.arity(); i++) {
			environment.define(this.declaration.params.get(i), args.get(i));
		}

		try {
			intp.executeBlock(this.declaration.body, environment);
		} catch (Return ret) {
			return ret.value;
		}
		if (this.isInitializer)
			return this.closure.getAt(0, "this");

		return null;
	}

	@Override
	public String toString() {
		return "<fn " + this.declaration.name.lexeme + ">";
	}

	LoxFunction bind(LoxInstance instance) {
		var environment = new Environment(this.closure);
		environment.define("this", instance);
		return new LoxFunction(this.declaration, environment, this.isInitializer, class_);
	}
}