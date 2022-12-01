HALT = 99

def make_tape(str_tape):
    return [int(v) for v in str_tape.strip().split(',')]

_ALL_OPS = []
YIELD = object()

class StdioEnv:
    outputs = ()

    def reset(self):
        pass

    def input(self):
        return int(input('Requesting program input> '))

    def output(self, value):
        print("Output:", value)


class ProgrammaticEnv:
    def __init__(self, inputs):
        self.inputs = list(inputs)
        self.reset()

    def reset(self):
        self._iter = iter(self.inputs)
        self.outputs = []

    def input(self):
        return int(next(self._iter))

    def output(self, value):
        self.outputs.append(value)

    def __repr__(self):
        return f"ProgrammaticEnv(outputs={self.outputs})"


class Tape:
    def __init__(self, data):
        self.data = data

    def __getitem__(self, position):
        if isinstance(position, slice):
            # This should allow enforcing the below logic for slicing, which is
            # mainly only used to get interpreter arguments; it might be important
            # that for instance an instruction is inferring data from a "larger" tape
            self.ensure_size(max(position.start, position.stop))
            return self.data[position]

        if position < 0:
            raise Exception("Negative tape index")
        elif position >= len(self.data):
            return 0
        else:
            return self.data[position]

    def __setitem__(self, position, value):
        if position < 0:
            raise Exception("Setting negative tape index")
        self.ensure_size(position)
        self.data[position] = value

    def ensure_size(self, size):
        if size >= len(self.data):
            self.data += [0] * (1 + size - len(self.data))

    def __repr__(self):
        return f'Tape{self.data}'


class CallContext:
    def __init__(self, interpreter_ctx, argspec, op, call_mode, inputs):
        self.interpreter_ctx = interpreter_ctx
        self.tape = interpreter_ctx.tape
        self.env = interpreter_ctx.env
        self.mode = call_mode
        self.inputs = inputs
        self.argspec = argspec
        self.op = op

    def __getitem__(self, index):
        argspec = self.argspec[index]
        literal = self.inputs[index]
        if argspec is LITERAL:
            return literal
        elif argspec is ADDRESS:
            return self.resolve_address(index)
        else:
            return self.resolve_position(index)

    def resolve_address(self, index):
        mode = self.mode[index]
        literal = self.inputs[index]
        if mode == 2:
            return self.interpreter_ctx.relative_base + literal
        else:
            return literal

    def resolve_position(self, index):
        mode = self.mode[index]
        if mode == 1:
            return self.inputs[index]
        else:
            return self.tape[self.resolve_address(index)]

    def __len__(self):
        return len(self.inputs)

    def arg_repr(self, index):
        argspec = self.argspec[index]
        literal = self.inputs[index]
        mode = self.mode[index]
        if argspec is LITERAL:
            return f'{literal}L'
        elif argspec is ADDRESS:
            if mode == 2:
                position = literal + self.interpreter_ctx.relative_base
                return f'[{position} = {literal}R{self.interpreter_ctx.relative_base}](->{self.tape[position]})'
            else:
                position = literal
                return f'[{position}](->{self.tape[position]})'
        else:
            mode = self.mode[index]
            if mode == 2:
                position = literal + self.interpreter_ctx.relative_base
                return f'[{position} = {literal}R{self.interpreter_ctx.relative_base}](->{self.tape[position]})'
            elif mode == 1:
                return f'{literal}L'
            else:
                position = literal
                return f'[{position}](->{self.tape[position]})'

    def __repr__(self):
        return f'CallContext: {self.op.__name__} {" ".join(self.arg_repr(i) for i in range(len(self)))}'


class InterpreterContext:
    def __init__(self, tape, env, index=0, state=None, trampoline=False, debug=False):
        self.tape = Tape(tape)
        self.env = env
        self.state = state
        self.debug = debug
        self.trampoline = trampoline
        self.index = index
        self.relative_base = 0

    def __repr__(self):
        return f'''
            InterpreterContext(
                index={self.index},
                state={self.state},
                relative_base={self.relative_base},
                tape={self.tape},
            )
            '''

class IntcodeInterpreter:
    def __init__(self):
        self.registered_ops = {}
        self.register_default_ops(_ALL_OPS)

    def evaluate(self, tape, env=StdioEnv(), trampoline=False, debug=False):
        tape = list(tape)
        ctx = InterpreterContext(tape, env, trampoline=trampoline, debug=debug)
        ctx.env.reset()
        return self.evaluate_from_context(ctx)

    def evaluate_from_context(self, ctx):
        ctx.state = 'RUNNING'
        tape = ctx.tape
        debug = ctx.debug
        index = ctx.index
        while tape[index] != HALT:
            opcode = tape[index]
            opcode_base = opcode % 100
            op, argspec = self.registered_ops[opcode_base]
            argcount = len(argspec)
            mode_v = (opcode - (opcode % 100)) // 100
            mode_s = str(mode_v)
            padded_mode_s = '0' * (argcount - len(mode_s)) + mode_s
            mode = [int(c) for c in reversed(padded_mode_s)]
            next_index = index + argcount + 1
            call_ctx = CallContext(ctx, argspec, op, mode, tape[index+1:next_index])
            if debug:
                print(call_ctx)
            ret = op(call_ctx)
            index = next_index if ret is None else ret
            ctx.index = index

            # Handle trampoline YIELD cases
            if op is output and ctx.trampoline:
                ctx.state = YIELD
                return ctx

        ctx.state = HALT
        return ctx

    def register_default_ops(self, all_ops):
        for fn, opcode, argspec, is_official in all_ops:
            if is_official:
                self.registered_ops[opcode] = (fn, argspec)


def register_op(opcode: int, args, is_official=True):
    def decorator(fn):
        _ALL_OPS.append((fn, opcode, args, is_official))
        return fn
    return decorator

LITERAL, ADDRESS, L_OR_A = object(), object(), object()

@register_op(opcode=1, args=[L_OR_A, L_OR_A, ADDRESS])
def add(ctx):
    a, b, oi = ctx
    ctx.tape[oi] = a + b

@register_op(opcode=2, args=[L_OR_A, L_OR_A, ADDRESS])
def multiply(ctx):
    a, b, oi = ctx
    ctx.tape[oi] = a * b

@register_op(opcode=3, args=[ADDRESS])
def _input(ctx):
    (oi,) = ctx
    ctx.tape[oi] = int(ctx.env.input())

@register_op(opcode=4, args=[L_OR_A])
def output(ctx):
    (v,) = ctx
    ctx.env.output(v)

@register_op(opcode=5, args=[L_OR_A, L_OR_A])
def jump_if_true(ctx):
    v, ji = ctx
    if v:
        return ji

@register_op(opcode=6, args=[L_OR_A, L_OR_A])
def jump_if_false(ctx):
    v, ji = ctx
    if not v:
        return ji

@register_op(opcode=7, args=[L_OR_A, L_OR_A, ADDRESS])
def less_than(ctx):
    a, b, oi = ctx
    ctx.tape[oi] = int(a < b)

@register_op(opcode=8, args=[L_OR_A, L_OR_A, ADDRESS])
def equals(ctx):
    a, b, oi = ctx
    ctx.tape[oi] = int(a == b)

@register_op(opcode=9, args=[L_OR_A])
def set_relative_base(ctx):
    (base_update,) = ctx
    ctx.interpreter_ctx.relative_base += base_update
