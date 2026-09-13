package com.featuredeliveryintelligence.fdi.product.realization.methodcalibration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class QualifiedSourceCallsTests {
    @TempDir Path root;
    private SourceMethodIndex index(String text) throws Exception {
        Path source = root.resolve("Service.java"); Files.writeString(source, text);
        return new SourceMethodIndex(root, List.of(source));
    }
    @Test void exactArgumentTypeResolvesSourceDeclaredInterfaceMethod() throws Exception {
        var index = index("""
                package demo;
                interface Repo extends ExternalFramework { String lookup(String key); }
                class Service { Repo repo; void run(String key) { repo.lookup(key); } }
                """);
        assertEquals(List.of("demo.Repo#lookup(java.lang.String)"), signatures(index, "demo.Service#run", "FIND"));
    }
    @Test void inheritedOverloadIsResolvedInsteadOfChoosingWrongDeclaredMethod() throws Exception {
        var index = index("""
                package demo;
                class Parent { void save(String key) { work(); } void work() {} }
                class Child extends Parent { void save(Object key) {} }
                class Service { Child repo; void run() { repo.save("key"); } }
                """);
        assertEquals(List.of("demo.Parent#save(java.lang.String)"), signatures(index, "demo.Service#run", "CREATE"));
    }
    @Test void successSkipsCatchOnlyCallsAndTrivialPlumbing() throws Exception {
        var index = index("""
                package demo;
                class Service {
                  int id;
                  void run() { try { getId(); setId(1); work(); } catch (RuntimeException e) { duplicate(e); } }
                  int getId() { return id; }
                  void setId(int id) { this.id = id; }
                  void work() { System.out.println("work"); }
                  void duplicate(RuntimeException e) { System.out.println(e); }
                }
                """);
        assertEquals(List.of("demo.Service#work()"), signatures(index, "demo.Service#run", "CREATE"));
    }
    @Test void localShadowDoesNotBindFieldAndUnknownArgumentDoesNotGuess() throws Exception {
        var index = index("""
                package demo;
                class Repo { void lookup(String key) {} }
                class Service {
                  Repo repo;
                  void run(Object repo) { repo.lookup("x"); }
                  void other() { repo.lookup(unknown()); }
                }
                """);
        assertTrue(signatures(index, "demo.Service#run", "CREATE").isEmpty());
        assertTrue(signatures(index, "demo.Service#other", "CREATE").isEmpty());
    }
    private List<String> signatures(SourceMethodIndex index, String name, String action) {
        return new QualifiedSourceCalls(index).calls(index.unique(name), action).stream()
                .map(SourceMethodIndex.Method::signature).toList();
    }
    @Test void existingTargetUpdateDoesNotInheritAbsentTargetCreationFallback() throws Exception {
        var index = index("""
                package demo;
                class Item {}
                class Repo { Item find(int id){return null;} }
                class Service { Repo repo;
                  void run(Item input,int id){Item stored=repo.find(id);if(stored!=null){change(stored);}else{create(input);}}
                  void inverse(Item input,int id){Item stored=repo.find(id);if(null==stored){create(input);}else{change(stored);}}
                  void change(Item input){} void create(Item input){}
                }
                """);
        assertFalse(signatures(index,"demo.Service#run","UPDATE").contains("demo.Service#create(demo.Item)"));
        assertTrue(signatures(index,"demo.Service#run","UPDATE").contains("demo.Service#change(demo.Item)"));
        assertTrue(signatures(index,"demo.Service#run","CREATE").contains("demo.Service#create(demo.Item)"));
        assertFalse(signatures(index,"demo.Service#inverse","UPDATE").contains("demo.Service#create(demo.Item)"));
    }
    @Test void unrelatedNullDefaultIsNotAnAbsentTargetBranch() throws Exception {
        var index = index("""
                package demo; class Item {}
                class Service { String find(){return null;}
                  void run(Item input){String label=find();if(label==null){useDefault();}}
                  void useDefault(){}
                }
                """);
        assertTrue(signatures(index,"demo.Service#run","UPDATE").contains("demo.Service#useDefault()"));
    }
    @Test void reassignedLookupCannotSuppressItsNewValuesBranch() throws Exception {
        var index = index("""
                package demo; class Item{} class Repo{Item find(int id){return null;}}
                class Service{Repo repo;void run(Item input,int id){Item stored=repo.find(id);
                stored=null;if(stored==null){repair(input);}}void repair(Item input){} }
                """);
        assertTrue(signatures(index,"demo.Service#run","UPDATE").contains("demo.Service#repair(demo.Item)"));
    }
    @Test void unknownSuperclassCannotBeSilentlyReplacedByInterfaceDeclaration() throws Exception {
        var index = index("""
                package demo;
                import external.Base;
                interface I { void work(String key); }
                class C extends Base implements I {}
                class Service { C c; void run(){c.work("x");} }
                """);
        assertTrue(signatures(index, "demo.Service#run", "CREATE").isEmpty());
    }
    @Test void explicitLocalTypeSupportsCallsWithoutGuessingInitializerReturn() throws Exception {
        var index = index("""
                package demo;
                class Repo { void query(String key) {} }
                class Service { Repo repo; void run() { String key = external(); repo.query(key); } }
                """);
        assertEquals(List.of("demo.Repo#query(java.lang.String)"), signatures(index, "demo.Service#run", "FIND"));
    }
    @Test void successAndRejectionRespectEarlyErrorReturn() throws Exception {
        var index = index("""
                package demo;
                class Errors { boolean hasErrors(){return false;} }
                class Service { Errors errors;
                  void run() { check(); if(errors.hasErrors()) { failure(); return; } save(); }
                  void check(){} void failure(){} void save(){}
                }
                """);
        var success = signatures(index, "demo.Service#run", "CREATE");
        var rejected = signatures(index, "demo.Service#run", "REJECT");
        assertTrue(success.contains("demo.Service#save()"));
        assertFalse(success.contains("demo.Service#failure()"));
        assertTrue(rejected.contains("demo.Service#check()"));
        assertTrue(rejected.contains("demo.Service#failure()"));
        assertFalse(rejected.contains("demo.Service#save()"));
    }
    @Test void rejectionCannotFollowExplicitSuccessElseBranch() throws Exception {
        var index = index("""
                package demo; class Errors {boolean hasErrors(){return false;}}
                class Service { Errors errors;
                void run(){if(errors.hasErrors()){failure();return;}else{save();return;}}
                void failure(){} void save(){} }
                """);
        assertFalse(signatures(index,"demo.Service#run","REJECT").contains("demo.Service#save()"));
    }
    @Test void siblingBlockLocalDoesNotSupplyAnOutOfScopeArgumentType() throws Exception {
        var index = index("""
                package demo;
                class Repo { void query(String key) {} }
                class Service { Repo repo; void run() { { String key = "x"; } repo.query(key); } }
                """);
        assertTrue(signatures(index, "demo.Service#run", "FIND").isEmpty());
    }
    @Test void optionalBoxingResolvesOnlySoleVisibleCompatibleDeclaration() throws Exception {
        var index = index("""
                package demo;
                class Store { void lookup(Integer id){} }
                class Service { Store store; void run(int id){store.lookup(id);} }
                """);
        var method = index.unique("demo.Service#run");
        assertTrue(new QualifiedSourceCalls(index).calls(method, "FIND").isEmpty());
        assertEquals(List.of("demo.Store#lookup(java.lang.Integer)"), new QualifiedSourceCalls(index, true)
                .calls(method, "FIND").stream().map(SourceMethodIndex.Method::signature).toList());
    }
    @Test void boxingNeverWinsOverCompetingPrimitiveWideningOrUnknownInheritance() throws Exception {
        var index = index("""
                package demo;
                class Store { void lookup(Integer id){} void lookup(long id){} }
                class Unknown extends External {void lookup(Integer id){}}
                class Service { Store store; Unknown unknown;
                    void run(int id){store.lookup(id);unknown.lookup(id);} }
                """);
        assertTrue(new QualifiedSourceCalls(index, true).calls(index.unique("demo.Service#run"), "FIND").isEmpty());
    }
    @Test void exactMatchWinsAndUnboxingIsConservative() throws Exception {
        var index = index("""
                package demo;
                class Store { void lookup(Integer id){} void lookup(int id){} void save(int id){} }
                class Service { Store store; void run(int id){store.lookup(id);} void other(Integer id){store.save(id);} }
                """);
        var calls = new QualifiedSourceCalls(index, true);
        assertEquals(List.of("demo.Store#lookup(int)"), calls.calls(index.unique("demo.Service#run"), "FIND")
                .stream().map(SourceMethodIndex.Method::signature).toList());
        assertEquals(List.of("demo.Store#save(int)"), calls.calls(index.unique("demo.Service#other"), "UPDATE")
                .stream().map(SourceMethodIndex.Method::signature).toList());
    }
    @Test void implicitObjectOverloadsPreventFalseBoxingEdges() throws Exception {
        var index = index("""
                package demo;
                class Store { void wait(Integer id){} public boolean equals(int id){return true;} }
                class Service { Store store;
                  void run(int id) throws InterruptedException {store.wait(id);}
                  void other(Integer id){store.equals(id);} }
                """);
        var calls = new QualifiedSourceCalls(index, true);
        assertTrue(calls.calls(index.unique("demo.Service#run"), "FIND").isEmpty());
        assertTrue(calls.calls(index.unique("demo.Service#other"), "FIND").isEmpty());
    }
}
