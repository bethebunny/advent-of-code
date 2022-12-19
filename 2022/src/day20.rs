#![feature(iterator_try_collect)]

mod aoc_client;

fn decrypt(encrypted: &Vec<i64>, rounds: usize) -> [i64; 3] {
    // Values are not necessarily unique, so we need to keep track of which value
    // corresponds to which original index.
    let mut decrypted: Vec<(usize, i64)> = encrypted.iter().copied().enumerate().collect();
    let n = encrypted.len();
    for _ in 0..rounds {
        for i in 0..n {
            // There's a couple off-by-1 considerations here
            //  - Consider a list of length N and a value of N
            //      - value of N and value of N + 1 should move to different places
            //      - 0 should go to the same place
            //      - Should k*N always be a no-op for k integer? unclear still
            //          - Consider a list (x, y, k)
            //          - (x, y, 0) -> (x, y, 0) (~= (0, x, y)) clearly
            //          - then (x, y, 1) -> (x, 1, y)
            //          - and so (x, y, 2) -> (x, y, 2)
            //          - so actually k*(N - 1) goes to the same place for k positive
            //          - similarly (x, y, -1) -> (x, -1, y) and (x, y, -2) -> (x, y, -2)
            //          - so k*(N - 1) ~= 0 for k integer
            let pos = decrypted.iter().position(|(oi, _)| *oi == i).unwrap();
            let val = decrypted[pos];
            let new_pos = (pos as i64 + val.1).rem_euclid(n as i64 - 1) as usize;
            decrypted.remove(pos);
            decrypted.insert(new_pos, val);
        }
    }
    let zero_pos = decrypted.iter().position(|(_, v)| *v == 0).unwrap();
    let coordinate_positions = [zero_pos + 1000, zero_pos + 2000, zero_pos + 3000];
    coordinate_positions.map(|p| decrypted[p % n].1)
}

fn main() {
    let data = aoc_client::input(2022, 20).unwrap();
    let encrypted: Vec<i64> = data.lines().map(|l| l.parse()).try_collect().unwrap();

    let decrypted = decrypt(&encrypted, 1);
    println!("{}", decrypted.iter().sum::<i64>());

    let decryption_key = 811_589_153;
    let decrypted = decrypt(&encrypted.iter().map(|v| v * decryption_key).collect(), 10);
    println!("{}", decrypted.iter().sum::<i64>());
}
