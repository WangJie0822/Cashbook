@file:Suppress("unused")

package cn.wj.android.cashbook.data.enum

import android.os.Parcelable
import androidx.annotation.StringRes
import cn.wj.android.cashbook.R
import cn.wj.android.cashbook.base.ext.base.string
import kotlinx.parcelize.Parcelize

/**
 * 货币数据
 *
 * @param titleResId 货币说明文本资源 id
 * @param symbol 货币符号
 * @param flag 货币图标
 *
 * > [王杰](mailto:15555650921@163.com) 创建于2021/6/1
 */
@Parcelize
enum class CurrencyEnum(
    @StringRes val titleResId: Int,
    val symbol: String,
    val flag: String
) : Parcelable {

    CNY(R.string.currency_title_cny, "¥", "\uD83C\uDDE8\uD83C\uDDF3"),
    EUR(R.string.currency_title_eur, "€", "\uD83C\uDDEA\uD83C\uDDFA"),
    GBP(R.string.currency_title_gbp, "£", "\uD83C\uDDEC\uD83C\uDDE7"),
    HKD(R.string.currency_title_hkd, "$", "\uD83C\uDDED\uD83C\uDDF0"),
    JPY(R.string.currency_title_jpy, "¥", "\uD83C\uDDEF\uD83C\uDDF5"),
    USD(R.string.currency_title_usd, "$", "\uD83C\uDDFA\uD83C\uDDF8"),
    AED(R.string.currency_title_aed, "د.إ", "\uD83C\uDDE6\uD83C\uDDEA"),
    AFN(R.string.currency_title_afn, "؋", "\uD83C\uDDE6\uD83C\uDDEB"),
    ALL(R.string.currency_title_all, "L", "\uD83C\uDDE6\uD83C\uDDF1"),
    AMD(R.string.currency_title_amd, "֏", "\uD83C\uDDE6\uD83C\uDDF2"),
    AOA(R.string.currency_title_aoa, "Kz", "\uD83C\uDDE6\uD83C\uDDF4"),
    ARS(R.string.currency_title_ars, "$", "\uD83C\uDDE6\uD83C\uDDF7"),
    AUD(R.string.currency_title_aud, "$", "\uD83C\uDDE6\uD83C\uDDFA"),
    AZN(R.string.currency_title_azn, "m", "\uD83C\uDDE6\uD83C\uDDFF"),
    BAM(R.string.currency_title_bam, "KM", "\uD83C\uDDE7\uD83C\uDDE6"),
    BBD(R.string.currency_title_bbd, "$", "\uD83C\uDDE7\uD83C\uDDE7"),
    BDT(R.string.currency_title_bdt, "Tk", "\uD83C\uDDE7\uD83C\uDDE9"),
    BGN(R.string.currency_title_bgn, "лв", "\uD83C\uDDE7\uD83C\uDDEC"),
    BHD(R.string.currency_title_bhd, "BD", "\uD83C\uDDE7\uD83C\uDDED"),
    BIF(R.string.currency_title_bif, "FBu", "\uD83C\uDDE7\uD83C\uDDEE"),
    BMD(R.string.currency_title_bmd, "$", "\uD83C\uDDE7\uD83C\uDDF2"),
    BND(R.string.currency_title_bnd, "$", "\uD83C\uDDE7\uD83C\uDDF3"),
    BOB(R.string.currency_title_bob, "Bs", "\uD83C\uDDE7\uD83C\uDDF4"),
    BRL(R.string.currency_title_brl, "R$", "\uD83C\uDDE7\uD83C\uDDF7"),
    BSD(R.string.currency_title_bsd, "B$", "\uD83C\uDDE7\uD83C\uDDF8"),
    BTN(R.string.currency_title_btn, "Nu.", "\uD83C\uDDE7\uD83C\uDDF9"),
    BWP(R.string.currency_title_bwp, "P", "\uD83C\uDDE7\uD83C\uDDFC"),
    BYN(R.string.currency_title_byn, "Br", "\uD83C\uDDE7\uD83C\uDDFE"),
    BZD(R.string.currency_title_bzd, "BZ$", "\uD83C\uDDE7\uD83C\uDDFF"),
    CAD(R.string.currency_title_cad, "$", "\uD83C\uDDE8\uD83C\uDDE6"),
    CDF(R.string.currency_title_cdf, "FC", "\uD83C\uDDE8\uD83C\uDDE9"),
    CHF(R.string.currency_title_chf, "CHF", "\uD83C\uDDE8\uD83C\uDDED"),
    CLP(R.string.currency_title_clp, "$", "\uD83C\uDDE8\uD83C\uDDF1"),
    COP(R.string.currency_title_cop, "$", "\uD83C\uDDE8\uD83C\uDDF4"),
    CRC(R.string.currency_title_crc, "₡", "\uD83C\uDDE8\uD83C\uDDF7"),
    CUP(R.string.currency_title_cup, "$", "\uD83C\uDDE8\uD83C\uDDFA"),
    CVE(R.string.currency_title_cve, "$", "\uD83C\uDDE8\uD83C\uDDFB"),
    CZK(R.string.currency_title_czk, "Kč", "\uD83C\uDDE8\uD83C\uDDFF"),
    DJF(R.string.currency_title_djf, "Fdj", "\uD83C\uDDE9\uD83C\uDDEF"),
    DKK(R.string.currency_title_dkk, "kr", "\uD83C\uDDE9\uD83C\uDDF0"),
    DOP(R.string.currency_title_dop, "RD$", "\uD83C\uDDE9\uD83C\uDDF4"),
    DZD(R.string.currency_title_dzd, "دج", "\uD83C\uDDE9\uD83C\uDDFF"),
    EGP(R.string.currency_title_egp, "£", "\uD83C\uDDEA\uD83C\uDDEC"),
    ERN(R.string.currency_title_ern, "نافكا", "\uD83C\uDDEA\uD83C\uDDF7"),
    ETB(R.string.currency_title_etb, "Br", "\uD83C\uDDEA\uD83C\uDDF9"),
    FJB(R.string.currency_title_fjb, "FJ$", "\uD83C\uDDEB\uD83C\uDDEF"),
    GEL(R.string.currency_title_gel, "₾", "\uD83C\uDDEC\uD83C\uDDEA"),
    GHS(R.string.currency_title_ghs, "¢", "\uD83C\uDDEC\uD83C\uDDED"),
    GMD(R.string.currency_title_gmd, "D", "\uD83C\uDDEC\uD83C\uDDF2"),
    GNF(R.string.currency_title_gnf, "FG", "\uD83C\uDDEC\uD83C\uDDF3"),
    GTQ(R.string.currency_title_gtq, "Q", "\uD83C\uDDEC\uD83C\uDDF9"),
    GYD(R.string.currency_title_gyd, "$", "\uD83C\uDDEC\uD83C\uDDFE"),
    HNL(R.string.currency_title_hnl, "L", "\uD83C\uDDED\uD83C\uDDF3"),
    HRK(R.string.currency_title_hrk, "kn", "\uD83C\uDDED\uD83C\uDDF7"),
    HTG(R.string.currency_title_htg, "G", "\uD83C\uDDED\uD83C\uDDF9"),
    HUF(R.string.currency_title_huf, "Ft", "\uD83C\uDDED\uD83C\uDDFA"),
    IDR(R.string.currency_title_idr, "Rp", "\uD83C\uDDEE\uD83C\uDDE9"),
    ILS(R.string.currency_title_ils, "₪", "\uD83C\uDDEE\uD83C\uDDF1"),
    INR(R.string.currency_title_inr, "₹", "\uD83C\uDDEE\uD83C\uDDF3"),
    IQD(R.string.currency_title_iqd, "ع.د", "\uD83C\uDDEE\uD83C\uDDF6"),
    IRR(R.string.currency_title_irr, "﷼", "\uD83C\uDDEE\uD83C\uDDF7"),
    ISK(R.string.currency_title_isk, "kr", "\uD83C\uDDEE\uD83C\uDDF8"),
    JMD(R.string.currency_title_jmd, "J$", "\uD83C\uDDEF\uD83C\uDDF2"),
    JOD(R.string.currency_title_jod, "د.ا", "\uD83C\uDDEF\uD83C\uDDF4"),
    KES(R.string.currency_title_kes, "KSh", "\uD83C\uDDF0\uD83C\uDDEA"),
    KGS(R.string.currency_title_kgs, "Лв", "\uD83C\uDDF0\uD83C\uDDEC"),
    KHR(R.string.currency_title_khr, "៛", "\uD83C\uDDF0\uD83C\uDDED"),
    KMF(R.string.currency_title_kmf, "CF", "\uD83C\uDDF0\uD83C\uDDF2"),
    KPW(R.string.currency_title_kpw, "₩", "\uD83C\uDDF0\uD83C\uDDF5"),
    KRW(R.string.currency_title_krw, "₩", "\uD83C\uDDF0\uD83C\uDDF7"),
    KWD(R.string.currency_title_kwd, "د.ك", "\uD83C\uDDF0\uD83C\uDDFC"),
    KYD(R.string.currency_title_kyd, "$", "\uD83C\uDDF0\uD83C\uDDFE"),
    KZT(R.string.currency_title_kzt, "лв", "\uD83C\uDDF0\uD83C\uDDFF"),
    LAK(R.string.currency_title_lak, "₭", "\uD83C\uDDF1\uD83C\uDDE6"),
    LBP(R.string.currency_title_lbp, "ل.ل.", "\uD83C\uDDF1\uD83C\uDDE7"),
    LKR(R.string.currency_title_lkr, "₨", "\uD83C\uDDF1\uD83C\uDDF0"),
    LRD(R.string.currency_title_lrd, "$", "\uD83C\uDDF1\uD83C\uDDF7"),
    LSL(R.string.currency_title_lsl, "M", "\uD83C\uDDF1\uD83C\uDDF8"),
    LTL(R.string.currency_title_ltl, "Lt", "\uD83C\uDDF1\uD83C\uDDF9"),
    LYD(R.string.currency_title_lyd, "ل.د", "\uD83C\uDDF1\uD83C\uDDFE"),
    MAD(R.string.currency_title_mad, "MAD", "\uD83C\uDDF2\uD83C\uDDE6"),
    MDL(R.string.currency_title_mdl, "MDL", "\uD83C\uDDF2\uD83C\uDDE9"),
    MGA(R.string.currency_title_mga, "Ar", "\uD83C\uDDF2\uD83C\uDDEC"),
    MKD(R.string.currency_title_mkd, "ден", "\uD83C\uDDF2\uD83C\uDDF0"),
    MMK(R.string.currency_title_mmk, "K", "\uD83C\uDDF2\uD83C\uDDF2"),
    MNT(R.string.currency_title_mnt, "₮", "\uD83C\uDDF2\uD83C\uDDF3"),
    MRO(R.string.currency_title_mro, "UM", "\uD83C\uDDF2\uD83C\uDDF7"),
    MUR(R.string.currency_title_mur, "₨", "\uD83C\uDDF2\uD83C\uDDF7"),
    MVR(R.string.currency_title_mvr, "Rf", "\uD83C\uDDF2\uD83C\uDDFB"),
    MWK(R.string.currency_title_mwk, "MK", "\uD83C\uDDF2\uD83C\uDDFC"),
    MXN(R.string.currency_title_mxn, "$", "\uD83C\uDDF2\uD83C\uDDFD"),
    MYR(R.string.currency_title_myr, "RM", "\uD83C\uDDF2\uD83C\uDDFE"),
    MZN(R.string.currency_title_mzn, "MT", "\uD83C\uDDF2\uD83C\uDDFF"),
    NAD(R.string.currency_title_mad, "$", "\uD83C\uDDF3\uD83C\uDDE6"),
    NGN(R.string.currency_title_ngn, "₦", "\uD83C\uDDF3\uD83C\uDDEC"),
    NIO(R.string.currency_title_nio, "C$", "\uD83C\uDDF3\uD83C\uDDEE"),
    NOK(R.string.currency_title_nok, "kr", "\uD83C\uDDF3\uD83C\uDDF4"),
    NPR(R.string.currency_title_npr, "₨", "\uD83C\uDDF3\uD83C\uDDF5"),
    NZD(R.string.currency_title_nzd, "$", "\uD83C\uDDF3\uD83C\uDDFF"),
    OMR(R.string.currency_title_omr, "﷼", "\uD83C\uDDF4\uD83C\uDDF2"),
    PAB(R.string.currency_title_pab, "B/.", "\uD83C\uDDF5\uD83C\uDDE6"),
    PEN(R.string.currency_title_pen, "S/.", "\uD83C\uDDF5\uD83C\uDDEA"),
    PGK(R.string.currency_title_pgk, "K", "\uD83C\uDDF5\uD83C\uDDEC"),
    PHP(R.string.currency_title_php, "₱", "\uD83C\uDDF5\uD83C\uDDED"),
    PKR(R.string.currency_title_pkr, "₨", "\uD83C\uDDF5\uD83C\uDDF0"),
    PLN(R.string.currency_title_pln, "zł", "\uD83C\uDDF5\uD83C\uDDF1"),
    PYG(R.string.currency_title_pyg, "Gs", "\uD83C\uDDF5\uD83C\uDDFE"),
    QAR(R.string.currency_title_qar, "﷼", "\uD83C\uDDF6\uD83C\uDDE6"),
    RON(R.string.currency_title_ron, "lei", "\uD83C\uDDF7\uD83C\uDDF4"),
    RSD(R.string.currency_title_rsd, "Дин.", "\uD83C\uDDF7\uD83C\uDDF8"),
    RUB(R.string.currency_title_rub, "₽", "\uD83C\uDDF7\uD83C\uDDFA"),
    RWF(R.string.currency_title_rwf, "FRw", "\uD83C\uDDF7\uD83C\uDDFC"),
    SAR(R.string.currency_title_sar, "﷼", "\uD83C\uDDF8\uD83C\uDDE6"),
    SBD(R.string.currency_title_sbd, "Si$", "\uD83C\uDDF8\uD83C\uDDE7"),
    SCR(R.string.currency_title_scr, "SR", "\uD83C\uDDF8\uD83C\uDDE8"),
    SDG(R.string.currency_title_sdg, "ج.س.", "\uD83C\uDDF8\uD83C\uDDE9"),
    SEK(R.string.currency_title_sek, "kr", "\uD83C\uDDF8\uD83C\uDDEA"),
    SGD(R.string.currency_title_sgd, "$", "\uD83C\uDDF8\uD83C\uDDEC"),
    SLL(R.string.currency_title_sll, "Le", "\uD83C\uDDF8\uD83C\uDDF1"),
    SOS(R.string.currency_title_sos, "S", "\uD83C\uDDF8\uD83C\uDDF4"),
    SRD(R.string.currency_title_srd, "$", "\uD83C\uDDF8\uD83C\uDDF7"),
    SSP(R.string.currency_title_ssp, "£", "\uD83C\uDDF8\uD83C\uDDF8"),
    STD(R.string.currency_title_std, "Db", "\uD83C\uDDF8\uD83C\uDDF9"),
    SYP(R.string.currency_title_syp, "LS", "\uD83C\uDDF8\uD83C\uDDFE"),
    SZL(R.string.currency_title_szl, "E", "\uD83C\uDDF8\uD83C\uDDFF"),
    THB(R.string.currency_title_thb, "฿", "\uD83C\uDDF9\uD83C\uDDED"),
    TJS(R.string.currency_title_tjs, "ЅM", "\uD83C\uDDF9\uD83C\uDDEF"),
    TMT(R.string.currency_title_tmt, "T", "\uD83C\uDDF9\uD83C\uDDF2"),
    TND(R.string.currency_title_tnd, "د.ت", "\uD83C\uDDF9\uD83C\uDDF3"),
    TOP(R.string.currency_title_top, "PT", "\uD83C\uDDF9\uD83C\uDDF4"),
    TRY(R.string.currency_title_try, "₺", "\uD83C\uDDF9\uD83C\uDDF7"),
    TTD(R.string.currency_title_ttd, "TT$", "\uD83C\uDDF9\uD83C\uDDF9"),
    TWD(R.string.currency_title_twd, "NT$", "\uD83C\uDDF9\uD83C\uDDFC"),
    TZS(R.string.currency_title_tzs, "TSh", "\uD83C\uDDF9\uD83C\uDDFF"),
    UAH(R.string.currency_title_uah, "₴", "\uD83C\uDDFA\uD83C\uDDE6"),
    UGX(R.string.currency_title_ugx, "USh", "\uD83C\uDDFA\uD83C\uDDEC"),
    UYU(R.string.currency_title_uyu, "$", "\uD83C\uDDFA\uD83C\uDDFE"),
    UZS(R.string.currency_title_uzs, "so'm", "\uD83C\uDDFA\uD83C\uDDFF"),
    VEF(R.string.currency_title_vef, "Bs", "\uD83C\uDDFB\uD83C\uDDEA"),
    VND(R.string.currency_title_vnd, "₫", "\uD83C\uDDFB\uD83C\uDDF3"),
    VUV(R.string.currency_title_vuv, "VT", "\uD83C\uDDFB\uD83C\uDDFA"),
    WST(R.string.currency_title_wst, "SAT", "\uD83C\uDDFC\uD83C\uDDF8"),
    XAF(R.string.currency_title_xaf, "FCFA", "\uD83C\uDF0D"),
    XCD(R.string.currency_title_xcd, "$", "\uD83C\uDF0E"),
    XOF(R.string.currency_title_xof, "CFA", "\uD83C\uDF0D"),
    YER(R.string.currency_title_yer, "﷼", "\uD83C\uDDFE\uD83C\uDDEA"),
    ZAR(R.string.currency_title_zar, "R", "🇿🇦"),
    ZMW(R.string.currency_title_zmw, "ZK", "\uD83C\uDDFF\uD83C\uDDF2");

    val code: String
        get() = name

    val summary: String
        get() = R.string.currency_summary_format.string.format(flag, titleResId.string, code)

    companion object {

        fun fromCode(code: String?): CurrencyEnum? {
            return values().firstOrNull { currency -> currency.code == code }
        }
    }
}