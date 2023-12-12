def main():
    let data = StaticTuple[4, Int8]()
    let x = rebind[Int32](data)
    print(x)
