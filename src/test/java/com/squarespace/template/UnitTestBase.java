package com.squarespace.template;

import static com.squarespace.template.Constants.EMPTY_ARGUMENTS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.icu.text.NumberFormat;
import com.squarespace.v6.utils.JSONUtils;

/**
 * Methods to simplify writing tests against the JSON template package.
 */
class UnitTestBase {

  private static final boolean DEBUG = false;
  
  private static final PredicateTable predicateTable = new PredicateTable();
  
  private static final FormatterTable formatterTable = new FormatterTable();
  
  static {
    predicateTable.register(new CorePredicates());
    predicateTable.register(new MediaPredicates());
    predicateTable.register(new UnitTestPredicates());

    formatterTable.register(new CoreFormatters());
    formatterTable.register(new MediaFormatters());
    formatterTable.register(new UnitTestFormatters());

    // Used to tune the symbol table sizes.
    if (DEBUG) {
      System.out.println("\nINSTRUCTION TABLE:");
      InstructionTable.dump();
      System.out.println("\nPREDICATE TABLE:");
      predicateTable.dump();
      System.out.println("============================\n");
      System.out.println("\nFORMATTER TABLE:");
      formatterTable.dump();
      System.out.println("============================\n");
    }
  }
  
  /**
   * This instance is stateless so it can be reused across tests.
   */
  private CodeMaker maker = new CodeMaker();
  
  public JsonTemplateEngine compiler() {
    return new JsonTemplateEngine(formatterTable, predicateTable);
  }
  
  public CodeBuilder builder() {
    return new CodeBuilder();
  }
  
  public Context context(String raw) {
    return new Context(json(raw));
  }
  
  public JsonNode json(String raw) {
    return JSONUtils.decode(raw);
  }
  
  public CodeMachine machine() {
    return new CodeMachine();
  }
  
  public CodeMaker maker() {
    return maker;
  }
  
  public Formatter formatter(String name) {
    return formatterTable.get(new StringView(name));
  }

  public Predicate predicate(String name) {
    return predicateTable.get(new StringView(name));
  }
  
  public CodeList collector() {
    return new CodeList();
  }
  
  public Tokenizer tokenizer(String data) {
    return tokenizer(data, collector());
  }
  
  public Tokenizer tokenizer(String data, CodeSink sink) {
    return new Tokenizer(data, sink, formatterTable, predicateTable);
  }
  
  public String repr(Instruction inst) {
    return ReprEmitter.get(inst, true);
  }

  public String eval(Context ctx) {
    return ctx.buffer().toString();
  }
  
  public String commas(long num) {
    return NumberFormat.getInstance().format(num);
  }
  
  public String commas(double num) {
    return NumberFormat.getInstance().format(num);
  }
  
  /**
   * Execute the instruction on the given JSON data.  Return the execution context.
   */
  public Context execute(String jsonData, Instruction ... instructions) throws CodeExecuteException {
    Context ctx = new Context(JSONUtils.decode(jsonData));
    for (Instruction inst : instructions) {
      inst.invoke(ctx);
    }
    return ctx;
  }

  /**
   * Grab the buffer from the context and assert that it equals the expected value.
   */
  public void assertContext(Context ctx, String expected) {
    String result = ctx.buffer().toString();
    assertEquals(result, expected);
  }
  
  
  public String format(Formatter impl, String json) throws CodeException {
    return format(impl, EMPTY_ARGUMENTS, json);
  }
  
  public String format(Formatter impl, Arguments args, String json) throws CodeException {
    Context ctx = new Context(JSONUtils.decode(json));
    impl.validateArgs(args);
    impl.apply(ctx, args);
    return eval(ctx);
  }
  
  public void assertFormatter(Formatter impl, String json, String expected) throws CodeException {
    assertEquals(format(impl, EMPTY_ARGUMENTS, json), expected);
  }
  
  public void assertFormatter(Formatter impl, Arguments args, String json, String expected) throws CodeException {
    assertEquals(format(impl, args, json), expected);
  }

  public void assertInvalidArgs(Formatter impl, Arguments args) {
    try {
      impl.validateArgs(args);
      fail("Expected " + args + " to raise exception");
    } catch (ArgumentsException e) {
      // Expected
    }
  }

}
