from pathlib import Path
from typing import Dict


def define_type(base_name, name, fields: Dict):
    field_decls = '\n    '.join(f'final {typ} {name};'
                                for name, typ in fields.items())
    constr_args = ', '.join(f'{typ} {name}'
                            for name, typ in fields.items())
    constr_body = '\n      '.join(f'this.{name} = {name};'
                                  for name in fields.keys())
    return '''
  static class {n} extends {bn} {{
    {f}
    
    {n}({ca}) {{
      {cb}
    }}
    
    @Override
    <R> R accept(Visitor<R> visitor) {{
      return visitor.visit{n}{bn}(this);
    }}
  }}
  '''.format(n=name, bn=base_name, f=field_decls, ca=constr_args, cb=constr_body)

def define_visitor(base_name, types: Dict):
    ast_visitors = '\n    '.join(f'R visit{type_name}{base_name}({type_name} {base_name.lower()});'
                                 for type_name in types.keys())
    return '''\
  interface Visitor<R> {{
    {v}
  }}
'''.format(v=ast_visitors)

def define_ast(output_dir, base_name, types: Dict):
    with open(output_dir/f'{base_name}.java', encoding='utf-8', mode='w') as f:
        ast_classes = ''.join(define_type(base_name, type_name, fields)
                              for type_name, fields in types.items())
        visitor = define_visitor(base_name, types)
        f.write('''\
package com.craftinginterpreters.lox;

import java.util.List;

abstract class {bn} {{
{v}
  abstract<R> R accept(Visitor<R> visitor);
{acs}
}}
'''.format(bn=base_name, acs=ast_classes, v=visitor))


def main():
    output_dir = Path('..')/'src'/'com'/'craftinginterpreters'/'lox'
    define_ast(output_dir, 'Expr', {
        'Binary': {'left': 'Expr', 'operator': 'Token', 'right': 'Expr'},
        'Logical': {'left': 'Expr', 'operator': 'Token', 'right': 'Expr'},
        'Grouping': {'expression': 'Expr'},
        'Literal': {'value': 'Object'},
        'Unary': {'operator': 'Token', 'right': 'Expr'},
        'Variable': {'name': 'Token'},
        'Assign': {'name': 'Token', 'value': 'Expr'},
    })
    define_ast(output_dir, 'Stmt', {
        'Block': {'statements': 'List<Stmt>'},
        'Expression': {'expression': 'Expr'},
        'Print': {'expression': 'Expr'},
        'Var': {'name': 'Token', 'initializer': 'Expr'},
        'If': {'condition': 'Expr', 'thenBranch': 'Stmt', 'elseBranch': 'Stmt'},
        'While': {'condition': 'Expr', 'body': 'Stmt'},
    })

if __name__ == '__main__':
    main()