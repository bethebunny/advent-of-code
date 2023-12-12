from algorithm.functional import unroll
from math.math import max
from sys.info import sizeof, alignof
from sys.intrinsics import _mlirtype_is_eq
from utils.static_tuple import StaticTuple


fn _alignto(value: Int, align: Int) -> Int:
    return (value + align - 1) // align * align


# FIXME: Can't pass *Ts to a function parameter, only type parameter.
struct _UnionSize[*Ts: CollectionElement]():
    @staticmethod
    fn compute() -> Int:
        var size = 0

        @parameter
        fn each[i: Int]():
            size = max(size, _alignto(sizeof[Ts[i]](), alignof[Ts[i]]()))

        unroll[len(VariadicList(Ts)), each]()
        return size


struct _UnionTypeIndex[T: CollectionElement, *Ts: CollectionElement]:
    @staticmethod
    fn compute() -> Int16:
        var result = -1

        @parameter
        fn each[i: Int]():
            alias q = Ts[i]

            @parameter
            if _mlirtype_is_eq[q, T]():
                result = i

        unroll[len(VariadicList(Ts)), each]()
        return result


@value
struct Variant[*Ts: CollectionElement](CollectionElement):
    alias _type = StaticTuple[_UnionSize[Ts].compute(), Int8]
    var _impl: Self._type
    var _state: Int16

    fn __copyinit__(inout self, other: Self):
        self._impl = Self._type()
        self._state = other._state

        @parameter
        fn each[i: Int]():
            if self._state == i:
                alias T = Ts[i]
                var _extra_copy_unsafe = other._impl
                let _extra_impl_ptr = Pointer.address_of(
                    _extra_copy_unsafe
                ).address
                var _extra_ptr = AnyPointer[T]()
                _extra_ptr.value = __mlir_op.`pop.pointer.bitcast`[
                    _type = __mlir_type[
                        `!kgen.pointer<:`, CollectionElement, ` `, T, `>`
                    ]
                ](_extra_impl_ptr)
                # Should call __copyinit__ finally, then __moveinit__
                self._get_ptr[T]().emplace_value(
                    __get_address_as_lvalue(_extra_ptr.value)
                )

        unroll[len(VariadicList(Ts)), each]()

    fn _get_ptr[T: CollectionElement](inout self) -> AnyPointer[T]:
        constrained[Self._check[T]() != -1, "not a union element type"]()
        let ptr = Pointer.address_of(self).address
        var result = AnyPointer[T]()
        result.value = __mlir_op.`pop.pointer.bitcast`[
            _type = __mlir_type[
                `!kgen.pointer<:`, CollectionElement, ` `, T, `>`
            ]
        ](ptr)
        return result

    fn __init__[T: CollectionElement](inout self, owned value: T):
        self._impl = Self._type()
        self._state = Self._check[T]()
        self._get_ptr[T]().emplace_value(value ^)

    fn __del__(owned self):
        self._destroy_current()

    fn _destroy_current(inout self):
        @parameter
        fn each[i: Int]():
            if self._state == i:
                alias q = Ts[i]
                __get_address_as_owned_value(self._get_ptr[q]().value).__del__()

        unroll[len(VariadicList(Ts)), each]()

    fn set[T: CollectionElement](inout self, owned value: T):
        self._destroy_current()
        self._state = Self._check[T]()
        self._get_ptr[T]().emplace_value(value ^)

    fn isa[T: CollectionElement](self) -> Bool:
        return self._state == Self._check[T]()

    fn get[T: CollectionElement](inout self) -> T:
        debug_assert(self.isa[T](), "get: wrong variant type")
        return __get_address_as_lvalue(self._get_ptr[T]().value)

    @staticmethod
    fn _check[T: CollectionElement]() -> Int16:
        return _UnionTypeIndex[T, Ts].compute()
