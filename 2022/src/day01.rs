mod aoc_client;

fn main() {
    let data = aoc_client::input(2022, 1).unwrap();
    // A str.split() is a std::str::Split which doesn't support .as_slice, unlike std::split::Split
    // So we'll collect into a Vec.
    let lines: Vec<&str> = data.trim().split("\n").collect();
    let elf_packs = lines.split(|&s| s == "");
    // Collect so we can sort later
    let mut elf_calories: Vec<u32> = elf_packs.map(elf_carrying_calories).collect();

    println!("{}", elf_calories.iter().max().unwrap());
    elf_calories.sort();
    println!("{}", elf_calories.iter().rev().take(3).sum::<u32>());
}

fn elf_carrying_calories(pack: &[&str]) -> u32 {
    pack.iter().map(|&s| s.parse::<u32>().unwrap()).sum()
}
