#![feature(iter_array_chunks)]

mod aoc_client;

fn main() {
    let data = aoc_client::input(2022, 10).unwrap();
    let interesting_cycles = [20, 60, 100, 140, 180, 220];
    let mut register: i64 = 1;
    let mut register_values: Vec<Vec<i64>> = vec![];
    for line in data.lines() {
        match line.split(" ").collect::<Vec<&str>>().as_slice() {
            ["noop"] => register_values.push(vec![register]),
            ["addx", value] => {
                let d = value.parse::<i64>().unwrap();
                register_values.push(vec![register, register]);
                register += d;
            }
            _ => panic!("Bad instruction"),
        }
    }
    let all_values: Vec<i64> = register_values.iter().flatten().cloned().collect();
    all_values
        .iter()
        .enumerate()
        .for_each(|v| println!("{:?}", v));
    println!(
        "{}",
        interesting_cycles
            .iter()
            // Cycles are indexed by 1
            .map(|&cycle| cycle as i64 * all_values[cycle - 1])
            .sum::<i64>()
    );
    all_values
        .iter()
        .array_chunks::<40>()
        .map(|row| {
            row.iter()
                .enumerate()
                .map(|(x, &reg)| {
                    if (reg - x as i64).abs() <= 1 {
                        '#'
                    } else {
                        '.'
                    }
                })
                .collect::<String>()
        })
        .for_each(|row| println!("{}", row));
}
