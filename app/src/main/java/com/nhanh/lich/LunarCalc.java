package com.nhanh.lich;

/** Âm lịch Việt Nam (múi giờ UTC+7). */
public final class LunarCalc {
  private static final double TZ = 7.0;
  private static final String[] CAN = {"Giáp","Ất","Bính","Đinh","Mậu","Kỷ","Canh","Tân","Nhâm","Quý"};
  private static final String[] CHI = {"Tý","Sửu","Dần","Mão","Thìn","Tỵ","Ngọ","Mùi","Thân","Dậu","Tuất","Hợi"};

  private LunarCalc() {}

  static long jdFromDate(int d, int m, int y) {
    long a = (14 - m) / 12, yy = y + 4800 - a, mm = m + 12 * a - 3;
    return d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045;
  }

  static long newMoonDay(long k) {
    double T = k / 1236.85, T2 = T * T, T3 = T2 * T, dr = Math.PI / 180;
    double Jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * T2 - 0.000000155 * T3
        + 0.00033 * Math.sin((166.56 + 132.87 * T - 0.009173 * T2) * dr);
    double M = 359.2242 + 29.10535608 * k - 0.0000333 * T2 - 0.00000347 * T3;
    double Mp = 306.0253 + 385.81691806 * k + 0.0107306 * T2 + 0.00001236 * T3;
    double F = 21.2964 - 390.67050646 * k - 0.0016528 * T2 - 0.00000239 * T3;
    double C1 = (0.1734 - 0.000393 * T) * Math.sin(M * dr) + 0.0021 * Math.sin(2 * dr * M)
        - 0.4068 * Math.sin(Mp * dr) + 0.0161 * Math.sin(2 * dr * Mp) - 0.0004 * Math.sin(3 * dr * Mp)
        + 0.0104 * Math.sin(2 * dr * F) - 0.0051 * Math.sin(dr * (M + Mp)) - 0.0074 * Math.sin(dr * (M - Mp))
        + 0.0004 * Math.sin(dr * (2 * F + M)) - 0.0004 * Math.sin(dr * (2 * F - M)) - 0.0006 * Math.sin(dr * (2 * F + Mp))
        + 0.0010 * Math.sin(dr * (2 * F - Mp)) + 0.0005 * Math.sin(dr * (2 * Mp + M));
    double dt = T < -11 ? 0.001 + 0.000839 * T + 0.0002261 * T2 : -0.000278 + 0.000265 * T + 0.000262 * T2;
    return (long) Math.floor(Jd1 + C1 - dt + 0.5 + TZ / 24);
  }

  static int sunSeg(long jdn) {
    double T = (jdn - 2451545.5 - TZ / 24) / 36525, T2 = T * T, dr = Math.PI / 180;
    double M = 357.5291 + 35999.0503 * T - 0.0001559 * T2 - 0.00000048 * T2 * T;
    double L0 = 280.46645 + 36000.76983 * T + 0.0003032 * T2;
    double DL = (1.9146 - 0.004817 * T - 0.000014 * T2) * Math.sin(dr * M)
        + (0.019993 - 0.000101 * T) * Math.sin(dr * 2 * M) + 0.00029 * Math.sin(dr * 3 * M);
    double L = (L0 + DL) * dr;
    L = L - Math.PI * 2 * Math.floor(L / (Math.PI * 2));
    return (int) Math.floor(L / Math.PI * 6);
  }

  static long month11(int y) {
    long k = (long) Math.floor((jdFromDate(31, 12, y) - 2415021) / 29.530588853);
    long nm = newMoonDay(k);
    if (sunSeg(nm) >= 9) nm = newMoonDay(k - 1);
    return nm;
  }

  static int leapOffset(long a11) {
    long k = (long) Math.floor((a11 - 2415021.076998695) / 29.530588853 + 0.5);
    int i = 1, last;
    int arc = sunSeg(newMoonDay(k + i));
    do { last = arc; i++; arc = sunSeg(newMoonDay(k + i)); } while (arc != last && i < 14);
    return i - 1;
  }

  /** @return {ngày, tháng, năm, nhuận(0/1)} */
  public static int[] toLunar(int d, int m, int y) {
    long dn = jdFromDate(d, m, y);
    long k = (long) Math.floor((dn - 2415021.076998695) / 29.530588853);
    long ms = newMoonDay(k + 1);
    if (ms > dn) ms = newMoonDay(k);
    long a11 = month11(y), b11 = a11;
    int ly;
    if (a11 >= ms) { ly = y; a11 = month11(y - 1); } else { ly = y + 1; b11 = month11(y + 1); }
    int day = (int) (dn - ms + 1);
    int diff = (int) Math.floor((ms - a11) / 29.0);
    int leap = 0, lm = diff + 11;
    if (b11 - a11 > 365) {
      int lo = leapOffset(a11);
      if (diff >= lo) { lm = diff + 10; if (diff == lo) leap = 1; }
    }
    if (lm > 12) lm -= 12;
    if (lm >= 11 && diff < 4) ly -= 1;
    return new int[] {day, lm, ly, leap};
  }

  public static String yearName(int y) { return CAN[(y + 6) % 10] + " " + CHI[(y + 8) % 12]; }

  public static String dayName(int d, int m, int y) {
    long j = jdFromDate(d, m, y);
    return CAN[(int) ((j + 9) % 10)] + " " + CHI[(int) ((j + 1) % 12)];
  }
}
