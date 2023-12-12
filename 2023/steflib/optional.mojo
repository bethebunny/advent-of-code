@value
struct _NoneType(CollectionElement):
    pass


@value
struct Optional[T: CollectionElement](CollectionElement):
    # _NoneType comes first so its index is 0.
    # This means that Optionals that are 0-initialized will be None.
    alias _type = Variant[_NoneType, T]
    var _value: Self._type

    # TODO: without an explicit move this creates a copy, and also even if
    #       value is an rvalue
    fn __init__(inout self, owned value: T):
        self._value = Self._type(value ^)

    fn __init__(inout self, value: NoneType):
        self._value = Self._type(_NoneType())

    fn __bool__(self) -> Bool:
        return not self._value.isa[_NoneType]()

    fn value(inout self) -> T:
        return self._value.get[T]()
