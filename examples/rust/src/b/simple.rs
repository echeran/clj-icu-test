use crate::kalai;
pub fn add(a: i64, b: i64) -> i64 {
    return (a + b);
}
pub fn main() {
    let args: std::vec::Vec<String> = std::env::args().collect();
    {
        println!("{}", add(1, 2));
    }
}
