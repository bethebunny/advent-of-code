mod aoc_client;

fn main() {
    let data = aoc_client::input(2022, 1).unwrap();
    // I'd love to have fewer collects() but I don't know how
    let lines = data.trim().split("\n").collect::<Vec<_>>();
    let elf_packs = lines.split(|s| *s == "").collect::<Vec<_>>();
    let mut elf_calories = elf_packs
        .iter()
        // ss.iter() produces Iterator<Item = &&str> instead of &str
        // we can get the type we "want" with .cloned()
        .map(|ss| elf_carrying_calories(ss.iter().cloned()))
        .collect::<Vec<_>>();

    println!("{}", elf_calories.iter().max().unwrap());
    elf_calories.sort();
    // I don't know why I need this declaration (or .iter())
    // - when I try to inline it I get some unholy error about trait bounds
    let largest_3 = elf_calories[elf_calories.len() - 3..elf_calories.len()].iter();
    println!("{}", largest_3.sum::<u32>());
}

fn elf_carrying_calories<'a, I>(it: I) -> u32
where
    I: Iterator<Item = &'a str>,
{
    it.map(|s| s.parse::<u32>())
        .collect::<Result<Vec<u32>, _>>()
        .unwrap()
        .iter()
        .sum()
}
