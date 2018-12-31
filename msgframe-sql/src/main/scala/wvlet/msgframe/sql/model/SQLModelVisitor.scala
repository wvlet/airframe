package wvlet.msgframe.sql.model
import wvlet.msgframe.sql.model.SQLModel._

/**
  *
  */
trait SQLModelVisitor[R, Context] {

  def default(m: SQLModel, c: Context): R

  def visit(m: SQLModel, c: Context): R = {
    val result = m match {
      case m: Aggregate     => visitAggregate(m, c)
      case m: Filter        => visitFilter(m, c)
      case m: Intersect     => visitIntersect(m, c)
      case m: Project       => visitProject(m, c)
      case m: Table         => visitTable(m, c)
      case m: Limit         => visitLimit(m, c)
      case m: With          => visitWith(m, c)
      case m: CreateSchema  => visitCreateSchema(m, c)
      case m: CreateTable   => visitCreateTable(m, c)
      case m: CreateView    => visitCreateView(m, c)
      case m: DropSchema    => visitDropSchema(m, c)
      case m: DropTable     => visitDropTable(m, c)
      case m: DropView      => visitDropView(m, c)
      case m: CreateTableAs => visitCreateTableAs(m, c)
      case m: InsertInto    => visitInsertInto(m, c)
      case m: Delete        => visitDelete(m, c)
      case m: SQLModel =>
        default(m, c)
    }
    for (x <- m.children) {
      visit(x, c)
    }
    result
  }

  def visitAggregate(a: Aggregate, c: Context): R = default(a, c)
  def visitFilter(f: Filter, c: Context): R       = default(f, c)
  def visitIntersect(i: Intersect, c: Context): R = default(i, c)
  def visitProject(p: Project, c: Context): R     = default(p, c)
  def visitTable(t: Table, c: Context): R         = default(t, c)
  def visitLimit(l: Limit, c: Context): R         = default(l, c)
  def visitWith(w: With, c: Context): R           = default(w, c)

  // DDL
  def visitCreateSchema(cs: CreateSchema, c: Context): R   = default(cs, c)
  def visitCreateTable(ct: CreateTable, c: Context): R     = default(ct, c)
  def visitCreateTableAs(ct: CreateTableAs, c: Context): R = default(ct, c)
  def visitCreateView(view: CreateView, c: Context): R     = default(view, c)
  def visitDropSchema(d: DropSchema, c: Context): R        = default(d, c)
  def visitDropTable(d: DropTable, c: Context): R          = default(d, c)
  def visitDropView(view: DropView, c: Context): R         = default(view, c)
  def visitInsertInto(i: InsertInto, c: Context): R        = default(i, c)
  def visitDelete(d: Delete, c: Context): R                = default(d, c)
}
