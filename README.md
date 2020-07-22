# katbot

IRC bot written in Java.

## Factoid Language Syntax

### Invoking factoids

Factoids are invoked with `~factoidName`. You can also provide a target like `~~ yawkat factoidName`.

### Prefixes

- When a factoid value starts with `/me`, it is sent as an `ACTION` message.
- When a factoid value starts with `/send`, it is sent as a normal message, allowing `/me` to be escaped.
- When a factoid value matches another katbot command, such as `~person++`, it will invoke that command (with privileges of the original sender).

### Special variables

- `target` is the target of the factoid invocation. This is typically the sender, but can be changed with `~~ name`.
- `actor` is the nick that invoked the factoid.

### Expressions

```
expression_list ::=
    (concat_expression '\s'+)* concat_expression?
    
concat_expression ::=
    expression+

expression ::=
    invocation |
    exploded_invocation |
    literal

literal ::=
    '[^ ]+' | '"' ('[^"]' | '\"')* '"'

invocation ::=
    '${' expression_list '}'

exploded_invocation ::=
    '*${' expression_list '}'
```

A factoid is a single `expression_list`.

Expressions return *lists* of strings when evaluated.

- A `literal` returns exactly one element, that literal string. It may be quoted.
- An `invocation` invokes the *factoid* (not any other command) matching the `expression_list` it contains. The result of that factoid is then joined to a single string using spaces. If the factoid `cat` has the value `[a, b, c]`, `${cat}` will yield `[a b c]`.
- An `exploded_invocation` is the same as a `factoid`, except that it may return multiple strings. If the factoid `cat` has the value `[a, b, c]`, `*${cat}` will yield `[a, b, c]`.
- A `concat_expression` consists of multiple expressions next to each other without whitespace separating them. These are concatenated as follows, assuming the factoid `cat` has the value `[a, b, c]`:
    - `x${cat}y` yields `[xa b cy]`
    - `x*${cat}y` yields `[xa, b, cy]`
    - `*${cat}*${cat}` yields `[a, b, ca, b c]`

After a factoid is evaluated, its return string list is joined with spaces, similar to an `invocation` expression.

### Special functions

Invocations are pattern-matched on the following functions. These functions are lists with wildcard parameters in them, accepting any string or any list of strings at their place.

`$a` shall indicate a single-item parameter called `a`.

`*$a` shall indicate a variable-length parameter called `a`.

A value is *truthy* if it is not `0`, not blank and not `false`.

- `[if, $cond, $t, *$f]`: If `cond` is truthy, return `[t]`, else return `f`.
- `sum *$addends`: Returns the sum of the decimal numbers in the addends, or `[NaN]` if one or more addend is not a decimal number. Returns `[0]` when no addends are present.
- `[product, *$factors]`: Returns the product of the decimal numbers in the factors, or `[NaN]` if one or more factor is not a decimal number. Returns `[1]` when no factors are present.
- `[equal, *$items]`: Returns `true` if all items are equal or there are no items, `false` otherwise.
- `[lt, *$items]`, `[gt, *$items]`, `[leq, *$items]`, `[geq, *$items]` return `true` if all items are numbers and neighbouring pairs are less than, greater than, less than or equal, greater than or equal to each other, or `items` is empty. Returns `false` if this is not the case.
- `[random, *$items]` returns a random item from `items`.

### Factoid parameters

When a factoid is saved with `$` in the factoid name, such as `~cat $ = ...`, these parameters count as wildcards and match any string. These parameters are then passed to the factoid value evaluation and can be accessed through the invocations `${1}`, `${2}` and so on.

A trailing parameter may also match multiple space-separated strings, so that `*${1}` will yield a list with a size larger than or equal to 1.
