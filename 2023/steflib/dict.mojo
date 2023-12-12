from memory.anypointer import AnyPointer
from .optional import Optional
from collections.vector import CollectionElement


trait EqualsComparable:
    fn __eq__(self, other: Self) -> Bool:
        pass


trait KeyElement(CollectionElement, Hashable, EqualsComparable, Stringable):
    pass


@value
struct DictEntry[K: KeyElement, V: CollectionElement](CollectionElement):
    var hash: Int
    var key: K
    var value: V


struct DictIndex:
    var data: DTypePointer[DType.invalid]

    fn __init__(inout self, reserved: Int):
        if reserved <= 128:
            let data = DTypePointer[DType.int8].alloc(reserved)
            for i in range(reserved):
                data[i] = -1  # TODO: Dict.EMPTY
            self.data = data.bitcast[DType.invalid]()
        elif reserved <= 2**16 - 2:
            let data = DTypePointer[DType.int16].alloc(reserved)
            for i in range(reserved):
                data[i] = -1
            self.data = data.bitcast[DType.invalid]()
        elif reserved <= 2**32 - 2:
            let data = DTypePointer[DType.int32].alloc(reserved)
            for i in range(reserved):
                data[i] = -1
            self.data = data.bitcast[DType.invalid]()
        else:
            let data = DTypePointer[DType.int64].alloc(reserved)
            for i in range(reserved):
                data[i] = -1
            self.data = data.bitcast[DType.invalid]()

    fn get_index(self, reserved: Int, slot: Int) -> Int:
        if reserved <= 128:
            let data = self.data.bitcast[DType.int8]()
            return data.load(slot % reserved).to_int()
        elif reserved <= 2**16 - 2:
            let data = self.data.bitcast[DType.int16]()
            return data.load(slot % reserved).to_int()
        elif reserved <= 2**32 - 2:
            let data = self.data.bitcast[DType.int32]()
            return data.load(slot % reserved).to_int()
        else:
            let data = self.data.bitcast[DType.int64]()
            return data.load(slot % reserved).to_int()

    fn set_index(inout self, reserved: Int, slot: Int, value: Int):
        if reserved <= 128:
            let data = self.data.bitcast[DType.int8]()
            return data.store(slot % reserved, value)
        elif reserved <= 2**16 - 2:
            let data = self.data.bitcast[DType.int16]()
            return data.store(slot % reserved, value)
        elif reserved <= 2**32 - 2:
            let data = self.data.bitcast[DType.int32]()
            return data.store(slot % reserved, value)
        else:
            let data = self.data.bitcast[DType.int64]()
            return data.store(slot % reserved, value)

    fn __del__(owned self):
        self.data.free()


struct Dict[K: KeyElement, V: CollectionElement](Sized):
    alias EMPTY = -1
    alias REMOVED = -2

    var size: Int
    var n_entries: Int
    var reserved: Int

    var _index: DictIndex
    var data: DynamicVector[Optional[DictEntry[K, V]]]

    fn __init__(inout self):
        self.size = 0
        self.n_entries = 0
        self.reserved = 8
        self.data = DynamicVector[Optional[DictEntry[K, V]]](self.reserved)
        self._index = DictIndex(self.reserved)

    fn get(self, key: K, default: V) raises -> V:
        var value = self.find(key)
        return value.value() if value else default

    fn __getitem__(self, key: K) raises -> V:
        var value = self.find(key)
        if value:
            return value.value()
        else:
            raise "KeyError"

    fn __setitem__(inout self, key: K, value: V):
        self.insert(key, value)

    fn __contains__(self, key: K) -> Bool:
        # TODO: why not Bool()?
        return self.find(key).__bool__()

    fn __len__(self) -> Int:
        return self.size

    fn _get_index(self, slot: Int) -> Int:
        return self._index.get_index(self.reserved, slot)

    fn _set_index(inout self, slot: Int, index: Int):
        return self._index.set_index(self.reserved, slot, index)

    fn _next_index_slot(self, inout slot: Int, inout perturb: Int):
        alias PERTURB_SHIFT = 5
        perturb >>= 5
        slot = ((5 * slot) + perturb + 1) % self.reserved

    fn _find_empty_index(self, hash: Int) -> Int:
        # Return (found, slot, index)
        var slot = 0
        var perturb = hash
        for _ in range(self.reserved):
            self._next_index_slot(slot, perturb)
            let index = self._get_index(slot)
            if index == Self.EMPTY:
                return slot
        debug_assert(False, "no empty index in _find_empty_index")
        trap()
        return 0

    fn _find_index(self, hash: Int, key: K) -> (Bool, Int, Int):
        # Return (found, slot, index)
        var insert_slot: Int = -1
        var insert_index: Int = -1
        var slot = 0
        var perturb = hash
        for _ in range(self.reserved):
            self._next_index_slot(slot, perturb)
            let index = self._get_index(slot)
            if index == Self.EMPTY:
                return (False, slot, self.n_entries)
            elif index == Self.REMOVED:
                if insert_slot < 0:
                    insert_slot = slot
                    insert_index = self.n_entries
            else:
                var ev = self.data[index]
                debug_assert(ev.__bool__(), "entry in index must be full")
                let entry = ev.value()
                if hash == entry.hash and key == entry.key:
                    return (True, slot, index)

        return (False, insert_slot, insert_index)

    fn insert(inout self, key: K, value: V):
        self._maybe_resize()
        let hash = hash(key)
        let found: Bool
        let slot: Int
        let index: Int
        found, slot, index = self._find_index(hash, key)

        self.data[index] = Optional(DictEntry[K, V](hash, key ^, value ^))
        if not found:
            self._set_index(slot, index)
            self.size += 1
            self.n_entries += 1

    fn insert(inout self, owned entry: DictEntry[K, V]):
        self._maybe_resize()
        let found: Bool
        let slot: Int
        let index: Int
        found, slot, index = self._find_index(entry.hash, entry.key)

        self.data[index] = Optional(entry)
        if not found:
            self._set_index(slot, index)
            self.size += 1
            self.n_entries += 1

    fn find(self, key: K) -> Optional[V]:
        let hash = hash(key)
        let found: Bool
        let slot: Int
        let index: Int
        found, slot, index = self._find_index(hash, key)
        if found:
            var ev = self.data[index]
            debug_assert(ev.__bool__(), "entry in index must be full")
            let entry = ev.value()
            return Optional[V](entry.value)
        return Optional[V](None)

    fn pop(
        inout self, key: K, owned default: Optional[V] = Optional[V](None)
    ) raises -> V:
        let hash = hash(key)
        let found: Bool
        let slot: Int
        let index: Int
        found, slot, index = self._find_index(hash, key)
        if found:
            self._set_index(slot, Self.REMOVED)
            let ptr = self.data.data + index
            var ev = ptr.take_value()
            ptr.emplace_value(Optional[DictEntry[K, V]](None))
            self.size -= 1
            debug_assert(ev.__bool__(), "entry in index must be full")
            return ev.value().value
        elif default:
            return default.value()
        raise "KeyError"

    fn _maybe_resize(inout self):
        if 3 * self.size <= 2 * self.reserved:
            if self.n_entries + 2 >= self.reserved:
                self._compact()
            return
        let old_reserved = self.reserved
        print("resize!", self.size, "/", old_reserved, "->", old_reserved * 2)
        self.reserved *= 2
        self.size = 0
        self.n_entries = 0
        self._index = DictIndex(self.reserved)
        let old_data = self.data ^
        self.data = DynamicVector[Optional[DictEntry[K, V]]](self.reserved)
        for i in range(self.reserved):
            (self.data.data + i).emplace_value(Optional[DictEntry[K, V]](None))

        for i in range(old_reserved):
            var entry = (old_data.data + i).take_value()
            if entry:
                self.insert(entry.value())

    fn _compact(inout self):
        print("compact!")
        self._index = DictIndex(self.reserved)
        var right = 0
        for left in range(self.size):
            while True:
                var entry = (self.data.data + right).take_value()
                right += 1
                if entry:
                    let slot = self._find_empty_index(entry.value().hash)
                    self._set_index(slot, left)
                    (self.data.data + left).emplace_value(entry)
                    break

        self.n_entries = self.size

    fn print_index(self):
        print_no_newline("DictIndex[")
        for slot in range(self.reserved):
            if slot > 0:
                print_no_newline(", ")
            let index = self._get_index(slot)
            if index >= 0:
                print_no_newline(self._get_index(slot))
            else:
                print_no_newline("EMPTY" if index == Self.EMPTY else "REMOVED")
        print_no_newline("]")
        print()

    fn print(self):
        print("dict")
        self.print_index()
        for i in range(self.reserved):
            var ev = self.data[i]
            if ev:
                let entry = ev.value()
                print(i, entry.key)
                _ = entry ^
            else:
                print(i, "EMPTY")
        print("/dict")


# def test_basic():
#     var dict = Dict[String, Int]()
#     print(' ===  INSERT dict["a"] = 1 ===  ')
#     dict["a"] = 1

#     dict.print()
#     print(len(dict))

#     print(' ===  INSERT dict["b"] = 2 ===  ')
#     dict["b"] = 2
#     dict.print()

#     print(" ===  Retrieve ===  ")
#     print('dict["a"]:', dict["a"])
#     print('dict["b"]:', dict["b"])
#     _ = dict ^


# #     let entry = DictEntry[StringKey, Int](12, "entry", 2)
# #     print(entry.key)
# #     _ = entry ^


# def test_multiple_resizes():
#     var dict = Dict[String, Int]()
#     for i in range(20):
#         dict["key" + str(i)] = i + 1
#     dict.print()
#     print(dict["key10"])  # should be 11
#     print(dict["key19"])  # should be 20


# def test_compact():
#     var dict = Dict[String, Int]()
#     for i in range(20):
#         let key = "key" + str(i)
#         dict[key] = i + 1
#         dict.pop(key)
#     dict.print()


# def main():
#     print(hash(String("")))
#     print(hash(String("cheese and crackers")))
#     print(hash(String("This is 16 bytes")))
